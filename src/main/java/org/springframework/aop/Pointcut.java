package org.springframework.aop;


/**
 * 切点抽象
 * ClassFilter和MethodMatcher分别用于在不同的级别上限定JoinPoint的匹配范围
 * ClassFilter是限定在类级别上
 * MethodMatcher是限定在方法级别上
 * 由于PointCut需要同时匹配类和接口，所以需要同时包含ClassFilter和MethodMatcher
 * AspectJExpressionPointcut是对PointCut接口的实现
 * @author derekyi
 * @date 2020/12/5
 */
public interface Pointcut {

	ClassFilter getClassFilter();

	MethodMatcher getMethodMatcher();
}
