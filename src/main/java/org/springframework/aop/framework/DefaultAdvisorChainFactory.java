package org.springframework.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.AdvisedSupport;
import org.springframework.aop.Advisor;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.PointcutAdvisor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zqc
 * @date 2022/12/17
 */
public class DefaultAdvisorChainFactory implements AdvisorChainFactory {

	/**
	 * 查找一个代理对象的某个方法的拦截器链（除了原方法以外的其他所有增强方法）
	 * 在AdvisedSupport.advisors里面存储着所有的方法拦截器
	 * 但是这些方法拦截器不一定会拦截当前方法，比方说拦截器1只增强方法A，拦截器2只增强方法B
	 * 那我现在要找方法A的拦截器链，就要遍历AdvisedSupport.advisors，看里面哪些方法拦截器的切点表达式和当前方法匹配
	 * 注意这时返回的拦截器链可能是乱序的，可能第一个是AfterReturning，第二个是Before
	 */
	@Override
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(AdvisedSupport config, Method method, Class<?> targetClass) {
		// 获取到所有的Advisors，注意Advice有很多种，例如Before、After、AfterReturning、AfterThrowing
		Advisor[] advisors = config.getAdvisors().toArray(new Advisor[0]);
		// 最后返回的方法拦截器链
		List<Object> interceptorList = new ArrayList<>(advisors.length);
		Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());
		for (Advisor advisor : advisors) {
			if (advisor instanceof PointcutAdvisor) {
				// Add it conditionally.
				PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
				// 校验当前Advisor是否适用于当前对象
				if (pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {
					MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();
					boolean match;
					// 校验Advisor是否应用到当前方法上
					match = mm.matches(method, actualClass);
					if (match) {
						MethodInterceptor interceptor = (MethodInterceptor) advisor.getAdvice();
						interceptorList.add(interceptor);
					}
				}
			}
		}
		return interceptorList;
	}
}
