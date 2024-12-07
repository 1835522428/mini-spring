package org.springframework.aop.aspectj;

import org.aspectj.weaver.tools.PointcutExpression;
import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.PointcutPrimitive;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * @author derekyi
 * @date 2020/12/5
 */
public class AspectJExpressionPointcut implements Pointcut, ClassFilter, MethodMatcher {

	private static final Set<PointcutPrimitive> SUPPORTED_PRIMITIVES = new HashSet<PointcutPrimitive>();

	static {
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.EXECUTION);
	}

	//解析后的切点表达式
	private final PointcutExpression pointcutExpression;

	public AspectJExpressionPointcut(String expression) {
		//这里基本都是调用的第三方库，不用管，只需要知道pointcutExpression这东西能匹配类/方法就可以
		//如果pointcutExpression和方法匹配，该方法就包含在切点表达式里面，不匹配就不包含
		PointcutParser pointcutParser = PointcutParser.getPointcutParserSupportingSpecifiedPrimitivesAndUsingSpecifiedClassLoaderForResolution(SUPPORTED_PRIMITIVES, this.getClass().getClassLoader());
		pointcutExpression = pointcutParser.parsePointcutExpression(expression);
	}

	@Override
	public boolean matches(Class<?> clazz) {
		//判断切点表达式pointcutExpression是否包含类
		return pointcutExpression.couldMatchJoinPointsInType(clazz);
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		//判断切点表达式pointcutExpression是否包含targetClass类下面的方法method
		return pointcutExpression.matchesMethodExecution(method).alwaysMatches();
	}

	@Override
	public ClassFilter getClassFilter() {
		return this;
	}

	@Override
	public MethodMatcher getMethodMatcher() {
		return this;
	}
}
