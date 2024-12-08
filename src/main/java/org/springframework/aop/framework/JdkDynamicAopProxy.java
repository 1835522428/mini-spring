package org.springframework.aop.framework;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.AdvisedSupport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * JDK动态代理
 *
 * @author zqc
 * @date 2022/12/19
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
		//通过Proxy.newProxyInstance创建代理对象proxy，参数：类加载器、目标类、一个InvocationHandler（名为"h"）
		//代理对象proxy在执行被代理对象方法时（例如proxy.explode()）会直接调用h.invoke()方法
		return Proxy.newProxyInstance(getClass().getClassLoader(), advised.getTargetSource().getTargetClass(), this);
	}

	/**
	 * 本方法是在proxy的方法里面实际执行的h.invoke函数，h是InvocationHandler的简称
	 *
	 * 在这里面会调用方法拦截器链（MethodInterceptor Chain）里面的内容
	 * 拦截器链存储在AdvisedSupport.advisors属性里
	 * 每一个Advisor都包含两个东西：切点表达式 + MethodInterceptor
	 * 所以AdvisedSupport.advisors就是所有拦截器的总和，称为拦截器链
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// 获取目标对象
		Object target = advised.getTargetSource().getTarget();
		Class<?> targetClass = target.getClass();
		Object retVal = null;
		// 获取拦截器链
		List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
		if (chain == null || chain.isEmpty()) {
			return method.invoke(target, args);
		} else {
			// 将拦截器统一封装成ReflectiveMethodInvocation
			MethodInvocation invocation =
					new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
			// Proceed to the joinpoint through the interceptor chain.
			// 执行拦截器链
			retVal = invocation.proceed();
		}
		return retVal;
	}
}
