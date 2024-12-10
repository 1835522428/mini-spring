package org.springframework.test.aop;

import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.AdvisedSupport;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.TargetSource;
import org.springframework.aop.aspectj.AspectJExpressionPointcutAdvisor;
import org.springframework.aop.framework.CglibAopProxy;
import org.springframework.aop.framework.JdkDynamicAopProxy;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.adapter.AfterReturningAdviceInterceptor;
import org.springframework.aop.framework.adapter.MethodBeforeAdviceInterceptor;
import org.springframework.test.common.WorldServiceAfterReturnAdvice;
import org.springframework.test.common.WorldServiceBeforeAdvice;
import org.springframework.test.service.WorldService;
import org.springframework.test.service.WorldServiceImpl;

/**
 * @author derekyi
 * @date 2020/12/6
 */
public class DynamicProxyTest {

	private AdvisedSupport advisedSupport;

	/**
	 * 无论是使用JDK动态代理还是使用CGLIB动态代理都需要使用以下三个参数：
	 * 		被代理对象、方法拦截器（增强方法）、切点表达式
	 * 所以就准备了一个工具类AdvisedSupport用来存储这三个元素
	 * 而这实际上就是ProxyFactory里面需要的三个属性
	 */
	@Before
	public void setup() {
		WorldService worldService = new WorldServiceImpl();
		advisedSupport = new ProxyFactory();
		//Advisor是Pointcut和Advice的组合
		String expression = "execution(* org.springframework.test.service.WorldService.explode(..))";
		AspectJExpressionPointcutAdvisor advisor = new AspectJExpressionPointcutAdvisor();
		advisor.setExpression(expression);
		AfterReturningAdviceInterceptor methodInterceptor = new AfterReturningAdviceInterceptor(new WorldServiceAfterReturnAdvice());
		advisor.setAdvice(methodInterceptor);
		TargetSource targetSource = new TargetSource(worldService);
		advisedSupport.setTargetSource(targetSource);
		/*
			这里不再是advisedSupport.setMethodInterceptor了，换成了addAdvisor
			AdvisedSupport.advisors定义为了List<Advisor>，即从单一的拦截方法改为了拥有一个拦截器链
			一个Advisor里面有两个东西：切点表达式 + MethodInterceptor
			MethodInterceptor是在InvocationHandler.invoke方法里面调用的，用于执行实际的增强方法
			这里把一个AfterReturningAdviceInterceptor放入了AdvisedSupport.advisors
			就是给拦截器链存入了一个返回增强方法
		 */
		advisedSupport.addAdvisor(advisor);
	}

	/**
	 * JDK动态代理测试
	 * JDK动态代理是基于接口实现的，代理对象proxy需要实现被代理对象的接口
	 * @throws Exception
	 */
	@Test
	public void testJdkDynamicProxy() throws Exception {
		WorldService proxy = (WorldService) new JdkDynamicAopProxy(advisedSupport).getProxy();
		proxy.explode();
	}

	/**
	 * CGLIB动态代理测试
	 * CGLIB不需要依赖于接口，他直接继承自被代理对象
	 * @throws Exception
	 */
	@Test
	public void testCglibDynamicProxy() throws Exception {
		WorldService proxy = (WorldService) new CglibAopProxy(advisedSupport).getProxy();
		proxy.explode();
	}

	/**
	 * 代理工厂
	 * @throws Exception
	 */
	@Test
	public void testProxyFactory() throws Exception {
		/*
			ProxyFactory使用的是JDK动态代理还是CGLIB动态代理主要就取决于ProxyFactory.ProxyTargetClass属性
			为false就使用JDK动态代理，为true就使用CGLIB动态代理
		 */
		// 使用JDK动态代理
		ProxyFactory factory = (ProxyFactory) advisedSupport;
		factory.setProxyTargetClass(false);
		WorldService proxy = (WorldService) factory.getProxy();
		proxy.explode();

		// 使用CGLIB动态代理
		factory.setProxyTargetClass(true);
		proxy = (WorldService) factory.getProxy();
		proxy.explode();
	}

	@Test
	public void testBeforeAdvice() throws Exception {
		//设置BeforeAdvice
		String expression = "execution(* org.springframework.test.service.WorldService.explode(..))";
		AspectJExpressionPointcutAdvisor advisor = new AspectJExpressionPointcutAdvisor();
		advisor.setExpression(expression);
		MethodBeforeAdviceInterceptor methodInterceptor = new MethodBeforeAdviceInterceptor(new WorldServiceBeforeAdvice());
		advisor.setAdvice(methodInterceptor);
		//给拦截器链新增一个MethodBeforeAdviceInterceptor（前置增强）拦截方法
		advisedSupport.addAdvisor(advisor);
		ProxyFactory factory = (ProxyFactory) advisedSupport;
		WorldService proxy = (WorldService) factory.getProxy();
		proxy.explode();
	}

	@Test
	public void testAdvisor() throws Exception {
		WorldService worldService = new WorldServiceImpl();

		/*
			Advisor是Pointcut和Advice的组合
			Advisor里面存放了切点表达式和对应的增强方法
			生成了之后会保存在AdvisedSupport.advisors里面，存入拦截器链
		 */
		String expression = "execution(* org.springframework.test.service.WorldService.explode(..))";
		AspectJExpressionPointcutAdvisor advisor = new AspectJExpressionPointcutAdvisor();
		advisor.setExpression(expression);
		MethodBeforeAdviceInterceptor methodInterceptor = new MethodBeforeAdviceInterceptor(new WorldServiceBeforeAdvice());
		advisor.setAdvice(methodInterceptor);

		ClassFilter classFilter = advisor.getPointcut().getClassFilter();
		if (classFilter.matches(worldService.getClass())) {
			ProxyFactory proxyFactory = new ProxyFactory();

			TargetSource targetSource = new TargetSource(worldService);
			proxyFactory.setTargetSource(targetSource);
			proxyFactory.addAdvisor(advisor);
//			proxyFactory.setMethodMatcher(advisor.getPointcut().getMethodMatcher());
//			advisedSupport.setProxyTargetClass(true);   //JDK or CGLIB

			WorldService proxy = (WorldService) proxyFactory.getProxy();
			proxy.explode();
		}
	}
}
















