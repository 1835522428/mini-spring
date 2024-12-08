package org.springframework.test.aop;

import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.AdvisedSupport;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.TargetSource;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.framework.CglibAopProxy;
import org.springframework.aop.framework.JdkDynamicAopProxy;
import org.springframework.test.common.WorldServiceInterceptor;
import org.springframework.test.service.WorldService;
import org.springframework.test.service.WorldServiceImpl;

/**
 * @author derekyi
 * @date 2020/12/6
 */
public class DynamicProxyTest {

	private AdvisedSupport advisedSupport;

	/**
	 * 这里用到了一个@Before注解，要注意这个不是Spring的AOP里面的@Before注解
	 * 这个是JUnit测试框架的@Before注解，用于标记在每个测试方法执行之前需要运行的方法
	 * 它可以用来进行测试前的初始化操作，比如设置测试环境
	 *
	 * 实现动态代理有两种方式：基于JDK的动态代理和基于CGLIB的动态代理
	 * 他们都需要用到三个元素：被代理对象、方法拦截器（实际执行的代理方法）、切点表达式
	 * 这里为了方便使用，准备了一个类来存放这三个属性：AdvisedSupport类
	 *
	 * 所以这个@Before方法的作用就是把这三个元素统一提取出来，放到AdvisedSupport对象里面，便于后续使用
	 * （后续基于JDK的动态代理和基于CGLIB的动态代理两个测试类都需要用到，就可以提取出来）
	 *
	 * 那这时候有个问题，既然传入了代理对象，为什么还需要传入切点表达式呢
	 * 这是因为一个代理对象里面可能有很多个方法，有的方法需要被增强，有的方法不需要加强
	 * 切点表达式可以指明要加强的具体是哪些方法
	 * 代理对象中只有在切点表达式范围内的方法会执行方法拦截器中的内容，实现方法增强
	 */
	@Before
	public void setup() {
		WorldService worldService = new WorldServiceImpl();

		advisedSupport = new AdvisedSupport();
		/*
			被代理对象，根据这个创建代理对象proxy
			proxy会实现被代理对象的接口，从而获得被代理对象里面所有的方法
		 */
		TargetSource targetSource = new TargetSource(worldService);
		//方法拦截器，里面的invoke方法是代理对象实际执行的方法
		WorldServiceInterceptor methodInterceptor = new WorldServiceInterceptor();
		//切点表达式，指向WorldService.explode()方法
		MethodMatcher methodMatcher = new AspectJExpressionPointcut("execution(* org.springframework.test.service.WorldService.explode(..))").getMethodMatcher();
		//组装AdvisedSupport
		advisedSupport.setTargetSource(targetSource);
		advisedSupport.setMethodInterceptor(methodInterceptor);
		advisedSupport.setMethodMatcher(methodMatcher);
	}

	/**
	 * JDK动态代理实现
	 * @throws Exception
	 */
	@Test
	public void testJdkDynamicProxy() throws Exception {
		WorldService proxy = (WorldService) new JdkDynamicAopProxy(advisedSupport).getProxy();
		proxy.explode();
	}

	@Test
	public void testCglibDynamicProxy() throws Exception {
		WorldService proxy = (WorldService) new CglibAopProxy(advisedSupport).getProxy();
		proxy.explode();
	}
}
