package org.springframework.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.AdvisedSupport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * JDK动态代理
 *
 * 要使用JDK动态代理其实核心就是一件事：
 * 		通过Proxy.newProxyInstance创建代理对象proxy，其中代理对象proxy拥有被代理对象的所有方法
 * 调用Proxy.newProxyInstance方法时需要传递三个参数：
 * 		类加载器、被代理对象、一个InvocationHandler（记为"h"）
 *
 * 前两个参数很好理解，类加载器负责创建完代理对象proxy之后把他加载进来；
 * 那么这个InvocationHandler是个啥呢？这里可以结合已经生成的代理对象proxy的源码来看（可以通过Arthas查看proxy的源码）：
 *
 * 		public final class $Proxy2 extends Proxy implements WorldService {
 *
 * 		    public final void explode() {
 *				this.h.invoke(this, m3, null);
 * 		        return;
 * 		    }
 *
 * 		    static {
 * 				m3 = Class.forName("...WorldService").getMethod("explode", new Class[0]);
 * 			}
 *
 * 		}
 *
 * 第一行注意"implements WorldService"，说明代理对象获得了被代理对象的所有方法（本案例只有explode方法）
 * 代理对象proxy也要实现explode方法，所以DynamicProxyTest测试类中才能调用proxy.explode()
 *
 * 但是在proxy.explode()方法中，直接调用了h.invoke，这个"h"就是上面newProxyInstance传入的第三个参数
 * 由于"h"实现了InvocationHandler接口，所以他确实有invoke方法
 * 那么整个proxy.explode()方法相当于就是在执行h.invoke，可以说这个InvocationHandler.invoke就是代理对象proxy实际执行的内容
 * 所以如果这个h.invoke()函数是这么写的：
 * 		public Object invoke(Object proxy, Method method, Object[] args) {
 * 		 	return;	//直接返回
 * 		}
 * 那么不管被代理对象WorldServiceImpl里面的explode方法写的多么天花乱坠，调用proxy.explode都会直接返回，啥都不干
 * 但是一般都不会这么用代理对象，一般都是通过动态代理实现一些功能增强，比方说：
 * 		public Object invoke(Object proxy, Method method, Object[] args) {
 * 			doSomethingBeforeMethod();		//前置增强
 * 			method.invoke(被代理对象, args);	//由被代理对象执行原方法，注意这个invoke是反射包里的
 * 			doSomethingAfterMethod();		//后置增强
 * 			return;
 * 		}
 * 总的来说，代理对象proxy调用被代理对象方法（例如proxy.explode()）执行的内容完全取决于这个InvocationHandler.invoke()
 *
 * 所以要是想通过Proxy.newProxyInstance创建代理对象，就要先创建一个InvocationHandler
 * 在本案例中，当前类JdkDynamicAopProxy直接实现了InvocationHandler，所以newProxyInstance的第三个参数是this
 * 即当前类就是代理对象proxy执行被代理对象方法时，实际执行的那个InvocationHandler
 *
 * InvocationHandler.invoke()需要传三个参数：
 * 		代理对象proxy、被代理对象要执行的方法、被代理对象要执行的方法的方法参数
 * 继续看proxy的源码，调用h.invoke时传递了三个参数：
 * 		this（代理对象proxy）、m3（被代理对象的explode方法）、null（explode方法的参数，这里无参）
 * 可以发现h.invoke的传参和InvocationHandler.invoke需要的三个参数一致
 *
 * 在当前类实现的InvocationHandler.invoke方法中是这么处理的：
 * 		1.校验方法method是否匹配切点表达式
 * 		2.如果匹配，则执行一个增强方法MethodInterceptor
 * 		3.如果不匹配，由被代理对象执行原方法
 * 需要校验method是否匹配切点表达式是因为，被代理对象中有些方法需要增强，有些方法不需要，只有在切点表达式范围内的方法需要增强
 *
 * 参考视频教程：
 *		<a href="https://www.bilibili.com/video/BV1V2421w79R?vd_source=372cb7bcb4fc43f9f0d75663561ce23b&spm_id_from=333.788.player.switch">...</a>
 *
 * @author derekyi
 * @date 2020/12/5
 */
public class JdkDynamicAopProxy implements AopProxy, InvocationHandler {

	private final AdvisedSupport advised;

	public JdkDynamicAopProxy(AdvisedSupport advised) {
		this.advised = advised;
	}

	/**
	 * 返回代理对象
	 *
	 * @return
	 */
	@Override
	public Object getProxy() {
		//返回代理对象，传递参数：类加载器、要被代理的对象、InvocationHandler类型的对象（需要实现invoke方法）
		return Proxy.newProxyInstance(getClass().getClassLoader(), advised.getTargetSource().getTargetClass(), this);
	}

	/**
	 * InvocationHandler.invoke()方法实现
	 * @return
	 * @throws Throwable
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		//proxy的该方法是否匹配切点表达式（proxy里面有很多方法，有的匹配，有的不匹配）
		if (advised.getMethodMatcher().matches(method, advised.getTargetSource().getTarget().getClass())) {
			//如果匹配切点表达式，则执行代理方法
			MethodInterceptor methodInterceptor = advised.getMethodInterceptor();
			//三个参数：代理类、代理方法、参数
			//这里执行了实际的代理方法，注意这个invoke不是反射包里的，是自己写的
			return methodInterceptor.invoke(new ReflectiveMethodInvocation(advised.getTargetSource().getTarget(), method, args));
		}
		//如果不匹配切点表达式，则执行原方法，注意两个invoke不是一个包里面的函数，这个是反射包里面的，上面那个是自己写的
		return method.invoke(advised.getTargetSource().getTarget(), args);
	}
}
