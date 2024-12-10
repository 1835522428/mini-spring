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
	 * <p>
	 * 参数：proxy --- 代理对象	method --- 被代理对象的方法	args --- method的参数
	 * <p>
	 * 在这里面会调用方法拦截器链（MethodInterceptor Chain）里面的内容
	 * 拦截器链存储在AdvisedSupport.advisors属性里
	 * 每一个Advisor都包含两个东西：切点表达式 + MethodInterceptor
	 * 所以AdvisedSupport.advisors就是所有拦截器的总和，称为拦截器链
	 * <p>
	 * 这个invoke方法啰嗦了半天，其实就干了一件事情：运行拦截器链 invocation.proceed()
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// 获取目标对象
		Object target = advised.getTargetSource().getTarget();
		Class<?> targetClass = target.getClass();
		Object retVal = null;
		// 获取targetClass类的method方法的拦截器链
		List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
		/*
			如果拦截器链为空，则不需要生成代理对象，直接执行原方法（由被代理对象target执行）

			但是拦截器链实际上好像不可能为空，因为在createBean方法中生成bean时，
			只有当存在Advisor与当前bean匹配时，才会使用JDK或者CGLIB生成动态代理对象，并且保存对应Advisor到拦截器链
			如果没有Advisor与当前bean匹配，就不会生成代理对象，而直接生成bean的实例
			进到了这个invoke方法，一定是代理对象proxy的调用，既然已经生成了代理对象，拦截器链就不可能为空
		 */
		if (chain == null || chain.isEmpty()) {
			return method.invoke(target, args);
		} else {
			/*
				如果拦截器链不为空，则要生成动态代理对象，重点要看是怎么执行拦截器链的
				因为拦截器链本身是乱序的，有可能After方法在前，Before方法在后
				那到底怎么运行这个拦截器链呢，具体请看invocation.proceed()方法
				这里把拦截器链封装成了一个ReflectiveMethodInvocation
			 */
			MethodInvocation invocation =
					new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
			// Proceed to the joinpoint through the interceptor chain.
			// 执行拦截器链，具体怎么执行的去查看 ReflectiveMethodInvocation#proceed 方法！！！
			// 实际执行拦截器链就是挨个执行List里面的MethodInterceptor#invoke方法
			retVal = invocation.proceed();
		}
		return retVal;
	}
}
