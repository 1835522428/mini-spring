package org.springframework.aop.aspectj;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;

/**
 * aspectJ表达式的advisor
 *
 * @author derekyi
 * @date 2020/12/6
 */
public class AspectJExpressionPointcutAdvisor implements PointcutAdvisor {

	//切点表达式
	private AspectJExpressionPointcut pointcut;

	//Advice有很多种，例如Before、After、AfterReturning、AfterReturning
	private Advice advice;

	private String expression;

	public void setExpression(String expression) {
		this.expression = expression;
	}

	@Override
	public Pointcut getPointcut() {
		if (pointcut == null) {
			pointcut = new AspectJExpressionPointcut(expression);
		}
		return pointcut;
	}

	@Override
	public Advice getAdvice() {
		return advice;
	}

	public void setAdvice(Advice advice) {
		this.advice = advice;
	}
}
