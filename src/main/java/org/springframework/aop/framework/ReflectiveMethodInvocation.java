package org.springframework.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author zqc
 * @date 2022/12/16
 */
public class ReflectiveMethodInvocation implements MethodInvocation {

	protected final Object proxy;

	protected final Object target;

	protected final Method method;

	protected final Object[] arguments;

	protected final Class<?> targetClass;

	protected final List<Object> interceptorsAndDynamicMethodMatchers;

	private int currentInterceptorIndex = -1;

	public ReflectiveMethodInvocation(Object proxy,Object target, Method method, Object[] arguments,Class<?> targetClass,List<Object> chain) {
		this.proxy=proxy;
		this.target = target;
		this.method = method;
		this.arguments = arguments;
		this.targetClass=targetClass;
		// 方法拦截器链，里面装的全是MethodInterceptor类型
		this.interceptorsAndDynamicMethodMatchers=chain;
	}

	/**
	 * 拦截器链运行函数
	 * <p>
	 * 这个函数本身的逻辑很简单，就是从拦截器链下标为0开始，通过反射挨个运行拦截器链上记载的所有MethodInterceptor
	 * 但是有个大问题，拦截器链本身是乱序的，After方法有可能在Before方法之前，那直接运行一遍拦截器链是怎么保证运行顺序的呢
	 * 比方说现在拦截器链上一共就两个MethodInterceptor，下标0是AfterReturning方法，下标1是Before方法
	 * 从拦截器链下标0开始运行，进到AfterReturning方法拦截器，参考AfterReturningAdviceInterceptor，里面封装WorldServiceAfterReturnAdvice方法
	 * 在AfterReturningAdviceInterceptor#invoke方法中，是这么写的：
	 * ----------------------------------------------------------------------------------------------------
	 *  // AfterReturning方法拦截器
	 * 	public Object invoke(MethodInvocation mi) throws Throwable {
	 * 		Object retVal = mi.proceed();
	 * 		this.advice.afterReturning(retVal, mi.getMethod(), mi.getArguments(), mi.getThis());
	 * 		return retVal;
	 * 	}
	 * ----------------------------------------------------------------------------------------------------
	 * 这个后置执行方法居然首先执行了 mi.proceed()，而mi就是本方法（调用MethodInterceptor#invoke传参传的是this）
	 * 所以实际上暂时并没有执行第二句 this.advice.afterReturning 这个实际的后置方法，而是回到了this.proceed()
	 * 等回来之后，this.currentInterceptorIndex 这个属性已经是0了，再去执行proceed()方法，会把拦截器链中下标为1的MethodInterceptor拿出来
	 * 而这个MethodInterceptor是一个Before方法拦截器，参考MethodBeforeAdviceInterceptor，里面封装WorldServiceBeforeAdvice方法
	 * 此时执行MethodBeforeAdviceInterceptor.invoke(this)，去看看MethodBeforeAdviceInterceptor#invoke方法是这么写的：
	 * ----------------------------------------------------------------------------------------------------
	 *  // Before方法拦截器
	 * 	public Object invoke(MethodInvocation mi) throws Throwable {
	 * 		//在执行被代理方法之前，先执行before advice操作
	 * 		this.advice.before(mi.getMethod(), mi.getArguments(), mi.getThis());
	 * 		return mi.proceed();
	 * 	}
	 * ----------------------------------------------------------------------------------------------------
	 * 这里第一步直接执行了实际的前置方法！！！！！
	 * 前置增强方法执行完毕之后，执行 mi.proceed() 回到此方法，此时 this.currentInterceptorIndex 这个属性已经是1了
	 * 方法拦截器中所有方法调用完毕，去执行了 method.invoke 执行了被代理对象的原方法！！！
	 * 执行完毕之后，程序 return，但是！！！！return到哪里去呢！！！别忘了最开始的AfterReturning方法拦截器没执行完呢！！！
	 * 这个其实就是递归调用，最开始的AfterReturning方法拦截器被压在栈底了，等待 mi.proceed() 执行完毕（很巧妙的递归调用，是执行拦截器链的核心）
	 * 现在 mi.proceed() 终于执行完了，最后去执行了AfterReturning方法拦截器中的 this.advice.afterReturning，执行实际的后置增强方法！！！
	 * 这时，整个拦截器链运行完毕！！！返回到JdkDynamicAopProxy#invoke方法
	 * @return
	 * @throws Throwable
	 */
	@Override
	public Object proceed() throws Throwable {
		// 初始currentInterceptorIndex为-1，每调用一次proceed就把currentInterceptorIndex+1
		// 相等时说明拦截器链里面所有方法都执行过了
		if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
			// 当调用次数 = 拦截器个数时
			// 触发当前method方法（由被代理对象执行原方法）
			return method.invoke(this.target, this.arguments);
		}

		// 获取到下一个MethodInterceptor
		Object interceptorOrInterceptionAdvice =
				this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
		// 普通拦截器，直接触发拦截器invoke方法，注意这个invoke方法不是反射包里的，而是MethodInterceptor里定义的invoke方法
		return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
	}

	@Override
	public Method getMethod() {
		return method;
	}

	@Override
	public Object[] getArguments() {
		return arguments;
	}

	@Override
	public Object getThis() {
		return target;
	}

	@Override
	public AccessibleObject getStaticPart() {
		return method;
	}
}
