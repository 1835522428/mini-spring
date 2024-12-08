package org.springframework.test.common;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @author derekyi
 * @date 2020/12/6
 */
public class WorldServiceInterceptor implements MethodInterceptor {
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		System.out.println("Do something before the earth explodes");	//增强方法
		Object result = invocation.proceed();	//执行原有方法的代码，这个其实就是执行了method.invoke(target, arguments)
		System.out.println("Do something after the earth explodes");	//增强方法
		return result;
	}
}
