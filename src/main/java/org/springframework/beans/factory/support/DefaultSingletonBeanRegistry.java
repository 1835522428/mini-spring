package org.springframework.beans.factory.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;

/**
 * @author derekyi
 * @date 2020/11/22
 */
public class DefaultSingletonBeanRegistry implements SingletonBeanRegistry {

	 /**
	  * 一级缓存
	  */
	private Map<String, Object> singletonObjects = new HashMap<>();

	 /**
	  * 二级缓存
	  */
	private Map<String, Object> earlySingletonObjects = new HashMap<>();

	 /**
	  * 三级缓存
	  */
	private Map<String, ObjectFactory<?>> singletonFactories = new HashMap<String, ObjectFactory<?>>();

	private final Map<String, DisposableBean> disposableBeans = new HashMap<>();

	@Override
	public Object getSingleton(String beanName) {
		// 从一级缓存查找
		Object singletonObject = singletonObjects.get(beanName);
		if (singletonObject == null) {
			// 从二级缓存查找
			singletonObject = earlySingletonObjects.get(beanName);
			if (singletonObject == null) {
				ObjectFactory<?> singletonFactory = singletonFactories.get(beanName);
				if (singletonFactory != null) {
					// 每个bean都存储的singletonFactory#getObject方法都是不一样的，见doCreateBean方法
					// 实际上是在调用getEarlyBeanReference方法
					singletonObject = singletonFactory.getObject();
					//从三级缓存放进二级缓存
					earlySingletonObjects.put(beanName, singletonObject);
					singletonFactories.remove(beanName);
				}
			}
		}
		return singletonObject;
	}

	/**
	 * 将单例bean放入一级缓存中，再次需要获取单例bean时直接从一级缓存拿
	 * @param beanName
	 * @param singletonObject
	 */
	@Override
	public void addSingleton(String beanName, Object singletonObject) {
		singletonObjects.put(beanName, singletonObject); // 1
		earlySingletonObjects.remove(beanName); // 2
		singletonFactories.remove(beanName); // 3
	}

	protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
		singletonFactories.put(beanName, singletonFactory);
	}

	public void registerDisposableBean(String beanName, DisposableBean bean) {
		disposableBeans.put(beanName, bean);
	}

	public void destroySingletons() {
		ArrayList<String> beanNames = new ArrayList<>(disposableBeans.keySet());
		for (String beanName : beanNames) {
			DisposableBean disposableBean = disposableBeans.remove(beanName);
			try {
				disposableBean.destroy();
			} catch (Exception e) {
				throw new BeansException("Destroy method on bean with name '" + beanName + "' threw an exception", e);
			}
		}
	}
}
