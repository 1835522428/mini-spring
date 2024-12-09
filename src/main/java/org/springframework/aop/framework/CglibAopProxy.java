package org.springframework.aop.framework;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.springframework.aop.AdvisedSupport;

import java.lang.reflect.Method;

/**
 * CGLIB动态代理
 *
 * JDK动态代理需要依赖于接口实现
 * 在JDK动态代理的程序里面有详细说明：
 * JDK在生成代理对象proxy时，proxy的定义是"implements WorldService"
 * 这样proxy才能获取到被代理对象WorldServiceImpl里面的所有方法
 * 才能调用proxy.explode()
 *
 * 但是CGLIB不需要依赖于接口，基于CGLIB的动态代理在运行期间动态构建字节码的class文件
 * 为类生成子类，因此被代理类不需要继承自任何接口
 * 即CGLIB的代理类的定义应该类似于"extends WorldServiceImpl"
 *
 * @author derekyi
 * @date 2020/12/6
 */
public class CglibAopProxy implements AopProxy {

	/*
		自定义的一个工具类，里面有三个东西：
			被代理对象、增强方法、切点表达式（指明被代理对象中有哪些方法需要增强）
	 */
	private final AdvisedSupport advised;

	public CglibAopProxy(AdvisedSupport advised) {
		this.advised = advised;
	}


	@Override
	public Object getProxy() {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(advised.getTargetSource().getTarget().getClass());	//设置被代理对象
		enhancer.setInterfaces(advised.getTargetSource().getTargetClass());			//设置被代理对象的接口
		enhancer.setCallback(new DynamicAdvisedInterceptor(advised));				//回调接口
		return enhancer.create();
	}

	/**
	 * 注意此处的MethodInterceptor是cglib中的接口，advised中的MethodInterceptor的AOP联盟中定义的接口，因此定义此类做适配
	 */
	private static class DynamicAdvisedInterceptor implements MethodInterceptor {

		private final AdvisedSupport advised;

		private DynamicAdvisedInterceptor(AdvisedSupport advised) {
			this.advised = advised;
		}

		@Override
		public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
			CglibMethodInvocation methodInvocation = new CglibMethodInvocation(advised.getTargetSource().getTarget(), method, objects, methodProxy);
			//切点表达式判断
			if (advised.getMethodMatcher().matches(method, advised.getTargetSource().getTarget().getClass())) {
				//代理方法，但是这个invoke传递的参数和JDK动态代理里面的参数有点不一样
				//相同点是传递的一定是一个ReflectiveMethodInvocation，里面必须有一个proceed方法
				return advised.getMethodInterceptor().invoke(methodInvocation);
			}
			return methodInvocation.proceed();
		}
	}

	/**
	 * CglibMethodInvocation扩展ReflectiveMethodInvocation类以支持CGLIB代理
	 * 主要是覆盖了invokeJoinpoint()方法，如果有MethodProxy对象
	 * 则通过调用MethodProxy#invoke方法，否则通过反射调用
	 *
	 * 在WorldServiceInterceptor（增强方法）里invocation.proceed()执行的就是这里的内容
	 * 目的是执行被代理对象的原方法
	 * 在这里用了一个MethodProxy执行原方法，但是也可以直接通过反射执行：method.invoke(this.target, this.arguments)
	 */
	private static class CglibMethodInvocation extends ReflectiveMethodInvocation {

		private final MethodProxy methodProxy;

		public CglibMethodInvocation(Object target, Method method, Object[] arguments, MethodProxy methodProxy) {
			super(target, method, arguments);
			this.methodProxy = methodProxy;
		}

		@Override
		public Object proceed() throws Throwable {
			//通过MethodProxy调用被代理对象的方法
			//methodProxy.invoke是调用代理方法
			//methodProxy.invokeSuper是调用原生方法（CGLIB的proxy继承自被代理类，所以Super就是被代理类）
			return this.methodProxy.invoke(this.target, this.arguments);
		}
	}
}
