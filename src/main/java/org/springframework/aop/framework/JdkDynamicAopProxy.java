package org.springframework.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.AdvisedSupport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * JDK动态代理
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
		//是否匹配切点表达式
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
