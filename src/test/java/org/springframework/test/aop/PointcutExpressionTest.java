package org.springframework.test.aop;

import org.junit.Test;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.test.service.HelloService;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author derekyi
 * @date 2020/12/5
 */
public class PointcutExpressionTest {

	@Test
	public void testPointcutExpression() throws Exception {
		/*
			AspectJExpressionPointcut是对PointCut接口的实现
			用来解析切点表达式，判断类/方法是否满足切点表达式的覆盖范围
			里面具体的实现基本用的都是第三方库
		 */
		AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut("execution(* org.springframework.test.service.HelloService.*(..))");
		Class<HelloService> clazz = HelloService.class;
		Method method = clazz.getDeclaredMethod("sayHello");

		assertThat(pointcut.matches(clazz)).isTrue();
		assertThat(pointcut.matches(method, clazz)).isTrue();
	}
}
