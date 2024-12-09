package org.springframework.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.AdvisorChainFactory;
import org.springframework.aop.framework.DefaultAdvisorChainFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zqc
 * @date 2022/12/16
 */
public class AdvisedSupport {

	//是否使用cglib代理
	private boolean proxyTargetClass = true;

	private TargetSource targetSource;


	private MethodMatcher methodMatcher;

	private transient Map<Integer, List<Object>> methodCache;

	AdvisorChainFactory advisorChainFactory = new DefaultAdvisorChainFactory();

	//记录方法拦截器链，每一个Advisor都是一个MethodInterceptor
	private List<Advisor> advisors = new ArrayList<>();

	public AdvisedSupport() {
		this.methodCache = new ConcurrentHashMap<>(32);
	}
	public boolean isProxyTargetClass() {
		return proxyTargetClass;
	}

	public void setProxyTargetClass(boolean proxyTargetClass) {
		this.proxyTargetClass = proxyTargetClass;
	}

	public void addAdvisor(Advisor advisor) {
		advisors.add(advisor);
	}

	public List<Advisor> getAdvisors() {
		return advisors;
	}

	public TargetSource getTargetSource() {
		return targetSource;
	}

	public void setTargetSource(TargetSource targetSource) {
		this.targetSource = targetSource;
	}


	public MethodMatcher getMethodMatcher() {
		return methodMatcher;
	}

	public void setMethodMatcher(MethodMatcher methodMatcher) {
		this.methodMatcher = methodMatcher;
	}

	/**
	 * 用来返回方法的拦截器链，注意这时获得的拦截器链顺序可能是乱的
	 */
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Method method, Class<?> targetClass) {
		Integer cacheKey = method.hashCode();
		//methodCache是方法拦截器链的缓存
		List<Object> cached = this.methodCache.get(cacheKey);
		//如果没有缓存，则要去查找一边方法拦截器链然后放入缓存中
		if (cached == null) {
			cached = this.advisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(
					this, method, targetClass);
			this.methodCache.put(cacheKey, cached);
		}
		return cached;
	}
}
