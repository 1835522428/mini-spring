package org.springframework.test.aop;

import org.junit.Test;
import org.springframework.aop.AdvisedSupport;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.TargetSource;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.framework.JdkDynamicAopProxy;
import org.springframework.test.common.WorldServiceInterceptor;
import org.springframework.test.service.WorldService;
import org.springframework.test.service.WorldServiceImpl;

/**
 * @author derekyi
 * @date 2020/12/6
 */
public class DynamicProxyTest {

	@Test
	public void testJdkDynamicProxy() throws Exception {
		WorldService worldService = new WorldServiceImpl();
		/*
			创建实例advisedSupport，这个实例需要填充以下三个属性：
				TargetSource、MethodInterceptor、MethodMatcher
			Target里面就一个属性target，用来封装要被代理的对象
			MethodInterceptor提供了一个invoke方法，允许在被代理方法前后添加其他操作，参考WorldServiceInterceptor类
			方法拦截器是真正去执行代理功能的类
			MethodMatcher是切点表达式，指明要代理的对象范围

			其实这个测试类原理很简单，就是先有一个WorldServiceImpl对象
			然后通过某种方式生成了一个WorldServiceImpl的代理对象proxy
			在调用代理对象proxy.explode()方法时，转而去执行了WorldServiceInterceptor里面的内容
			WorldServiceInterceptor是对原对象的增强实现
			（动态代理其实就是：前面写一点增强方法+反射执行原方法+后面写一点增强方法）
		 */
		AdvisedSupport advisedSupport = new AdvisedSupport();
		//被代理对象的封装
		TargetSource targetSource = new TargetSource(worldService);
		WorldServiceInterceptor methodInterceptor = new WorldServiceInterceptor();
		//方法拦截器，可以在被代理执行的方法前后增加代理行为
		MethodMatcher methodMatcher = new AspectJExpressionPointcut("execution(* org.springframework.test.service.WorldService.explode(..))").getMethodMatcher();
		advisedSupport.setTargetSource(targetSource);
		advisedSupport.setMethodInterceptor(methodInterceptor);
		advisedSupport.setMethodMatcher(methodMatcher);

		WorldService proxy = (WorldService) new JdkDynamicAopProxy(advisedSupport).getProxy();
		/*
			表面上执行的是proxy.explode方法，实际执行的是JdkDynamicAopProxy.invoke方法

			proxy会实现被代理对象的接口，即WorldService接口，从而proxy能获取到被代理对象的所有方法
			所以proxy里面也是有explode方法的，但是这个explode方法里面会直接去执行InvocationHandler.invoke方法
			JdkDynamicAopProxy实现了InvocationHandler接口，所以执行的就是JdkDynamicAopProxy.invoke方法

			如果有兴趣的话可以让程序跑起来，然后用Arthas去查看这个proxy的源码
		 */
		proxy.explode();
	}
}
