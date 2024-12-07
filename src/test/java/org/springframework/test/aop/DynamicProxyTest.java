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
		proxy.explode();
	}
}
