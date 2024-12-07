package org.springframework.beans.factory.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.StringValueResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author derekyi
 * @date 2020/11/22
 */
public abstract class AbstractBeanFactory extends DefaultSingletonBeanRegistry implements ConfigurableBeanFactory {

	private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

	private final Map<String, Object> factoryBeanObjectCache = new HashMap<>();

	private final List<StringValueResolver> embeddedValueResolvers = new ArrayList<StringValueResolver>();

	private ConversionService conversionService;


	@Override
	public Object getBean(String name) throws BeansException {
		//去缓存中找 bean，第一次执行的时候都是找不到的
		Object sharedInstance = getSingleton(name);
		if (sharedInstance != null) {
			//如果是FactoryBean，从FactoryBean#getObject中创建bean
			return getObjectForBeanInstance(sharedInstance, name);
		}

		BeanDefinition beanDefinition = getBeanDefinition(name);
		//创建单例bean之后会把bean放入一级缓存
		Object bean = createBean(name, beanDefinition);
		return getObjectForBeanInstance(bean, name);
	}

	/**
	 * 如果是FactoryBean，从FactoryBean#getObject中创建bean
	 *
	 * @param beanInstance
	 * @param beanName
	 * @return
	 */
	protected Object getObjectForBeanInstance(Object beanInstance, String beanName) {
		Object object = beanInstance;
		if (beanInstance instanceof FactoryBean) {
			FactoryBean factoryBean = (FactoryBean) beanInstance;
			try {
				if (factoryBean.isSingleton()) {
					//singleton作用域bean，从缓存中获取
					object = this.factoryBeanObjectCache.get(beanName);
					if (object == null) {
						object = factoryBean.getObject();
						this.factoryBeanObjectCache.put(beanName, object);
					}
				} else {
					//prototype作用域bean，新创建bean
					object = factoryBean.getObject();
				}
			} catch (Exception ex) {
				throw new BeansException("FactoryBean threw exception on object[" + beanName + "] creation", ex);
			}
		}

		return object;
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		return ((T) getBean(name));
	}

	@Override
	public boolean containsBean(String name) {
		return containsBeanDefinition(name);
	}

	protected abstract boolean containsBeanDefinition(String beanName);

	protected abstract Object createBean(String beanName, BeanDefinition beanDefinition) throws BeansException;

	protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

	@Override
	public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
		//有则覆盖
		/*
			ArrayList.remove() 方法可以按照索引删除，也可以按照对象删除元素
			这里可以直接理解为新添加了一个 BeanPostProcessor

			对于每一个 Bean，都要经过所有 BeanPostProcessor 的处理，每个 BeanPostProcessor 有不同的职责
			在 Spring 中大部分 Bean 在创建时都要经过一些（可能不止一个） BeanPostProcessor
			所以创建 Bean 时所有的 BeanPostProcessor 都要过一遍
		 */
		this.beanPostProcessors.remove(beanPostProcessor);
		this.beanPostProcessors.add(beanPostProcessor);
	}

	public List<BeanPostProcessor> getBeanPostProcessors() {
		return this.beanPostProcessors;
	}

	public void addEmbeddedValueResolver(StringValueResolver valueResolver) {
		this.embeddedValueResolvers.add(valueResolver);
	}

	public String resolveEmbeddedValue(String value) {
		String result = value;
		for (StringValueResolver resolver : this.embeddedValueResolvers) {
			result = resolver.resolveStringValue(result);
		}
		return result;
	}

	@Override
	public ConversionService getConversionService() {
		return conversionService;
	}

	@Override
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}
}
