package org.springframework.beans.factory.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author derekyi
 * @date 2020/11/22
 */
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
		implements ConfigurableListableBeanFactory, BeanDefinitionRegistry {

	private Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

	@Override
	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
		beanDefinitionMap.put(beanName, beanDefinition);
	}

	@Override
	public BeanDefinition getBeanDefinition(String beanName) throws BeansException {
		BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
		if (beanDefinition == null) {
			throw new BeansException("No bean named '" + beanName + "' is defined");
		}

		return beanDefinition;
	}

	@Override
	public boolean containsBeanDefinition(String beanName) {
		return beanDefinitionMap.containsKey(beanName);
	}

	/**
	 * 获取BeanDefinitionMap中属于同一个类type的所有bean
	 * 实例化这些bean并返回
	 * 这里要注意一下Class的使用方式，举例来说：
	 * 		Class<Dog> dogClass = Dog.class;
	 * 或者直接：
	 * 		Class dogClass = Dog.class;
	 * 但是第二种方式失去了泛型提供的类型安全检查
	 * 这个方法使用了泛型主要是因为：
	 * 		T bean = (T) getBean(beanName);
	 * 这里需要使用泛型来接收实例，比方说这里要找到所有BeanFactoryPostProcessor类型的Bean
	 * 那么就需要：
	 * 		BeanFactoryPostProcessor bean = (BeanFactoryPostProcessor) getBean(beanName);
	 */
	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
		Map<String, T> result = new HashMap<>();
		beanDefinitionMap.forEach((beanName, beanDefinition) -> {
			Class beanClass = beanDefinition.getBeanClass();
			// 检查type类能否被分配给beanClass类，即beanClass类是否是type类的子类
			// 即所有type类的子类都会被选中
			if (type.isAssignableFrom(beanClass)) {
				T bean = (T) getBean(beanName);
				result.put(beanName, bean);
			}
		});
		return result;
	}

	public <T> T getBean(Class<T> requiredType) throws BeansException {
		List<String> beanNames = new ArrayList<>();
		for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
			Class beanClass = entry.getValue().getBeanClass();
			if (requiredType.isAssignableFrom(beanClass)) {
				beanNames.add(entry.getKey());
			}
		}
		if (beanNames.size() == 1) {
			return getBean(beanNames.get(0), requiredType);
		}

		throw new BeansException(requiredType + "expected single bean but found " +
				beanNames.size() + ": " + beanNames);
	}

	@Override
	public String[] getBeanDefinitionNames() {
		Set<String> beanNames = beanDefinitionMap.keySet();
		return beanNames.toArray(new String[beanNames.size()]);
	}

	@Override
	public void preInstantiateSingletons() throws BeansException {
		beanDefinitionMap.forEach((beanName, beanDefinition) -> {
			//只有当bean是单例且不为懒加载才会被创建（即默认饿汉式加载），创建之后放入一级缓存便于后面取用
			if (beanDefinition.isSingleton() && !beanDefinition.isLazyInit()) {
				getBean(beanName);
			}
		});
	}
}
