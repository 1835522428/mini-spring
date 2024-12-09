package org.springframework.test.common;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * 方法拦截器，必须要实现MethodInterceptor接口
 * 在动态代理中，代理方法实际执行的就是拦截器中的invoke方法
 * @author derekyi
 * @date 2020/12/6
 */
public class WorldServiceInterceptor implements MethodInterceptor {
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		System.out.println("Do something before the earth explodes");	//前置增强
		Object result = invocation.proceed();							//被代理对象执行原方法
		System.out.println("Do something after the earth explodes");	//后置增强
		return result;
	}
}
