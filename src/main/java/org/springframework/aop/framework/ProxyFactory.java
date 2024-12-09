package org.springframework.aop.framework;

import org.springframework.aop.AdvisedSupport;

/**
 * @author zqc
 * @date 2022/12/16
 */
public class ProxyFactory extends AdvisedSupport {


	public ProxyFactory() {
	}

	public Object getProxy() {
		return createAopProxy().getProxy();
	}

	private AopProxy createAopProxy() {
		/*
			根据proxyTargetClass属性的值选择生成动态代理的方式
			如果proxyTargetClass = false则选择JDK动态代理
			如果proxyTargetClass = true则选择CGLIB动态代理
		 */
		if (this.isProxyTargetClass() || this.getTargetSource().getTargetClass().length == 0) {
			return new CglibAopProxy(this);
		}

		return new JdkDynamicAopProxy(this);
	}
}
