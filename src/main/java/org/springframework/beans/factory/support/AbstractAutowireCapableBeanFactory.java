package org.springframework.beans.factory.support;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.TypeUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.*;
import org.springframework.core.convert.ConversionService;

import java.lang.reflect.Method;

/**
 * @author derekyi
 * @date 2020/11/22
 */
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory
		implements AutowireCapableBeanFactory {

	private InstantiationStrategy instantiationStrategy = new SimpleInstantiationStrategy();

	@Override
	protected Object createBean(String beanName, BeanDefinition beanDefinition) throws BeansException {
		/*
			如果bean需要代理，则直接返回代理对象
			resolveBeforeInstantiation这个方法直接尝试生成bean的代理对象
			如果没有和当前bean匹配的Advisor，就说明当前bean不需要增强，不需要代理对象，直接返回null

			作者在BUG FIX中修改了resolveBeforeInstantiation这个函数的代码
			现在这个函数必定返回null，无论怎样都会去执行doCreateBean方法，所以这部分可以忽略
			整个createBean方法可以简化为：
			----------------------------------------------------------------------------------------------------------
				protected Object createBean(String beanName, BeanDefinition beanDefinition) throws BeansException {
					return doCreateBean(beanName, beanDefinition);
				}
			----------------------------------------------------------------------------------------------------------
		 */
		Object bean = resolveBeforeInstantiation(beanName, beanDefinition);
		if (bean != null) {
			return bean;
		}
		// bean不需要代理，则直接创建bean实例
		return doCreateBean(beanName, beanDefinition);
	}

	/**
	 * 执行InstantiationAwareBeanPostProcessor的方法，如果bean需要代理，直接返回代理对象
	 *
	 * @param beanName
	 * @param beanDefinition
	 * @return
	 */
	protected Object resolveBeforeInstantiation(String beanName, BeanDefinition beanDefinition) {
		//传参：被代理对象的类以及beanName
		Object bean = applyBeanPostProcessorsBeforeInstantiation(beanDefinition.getBeanClass(), beanName);
		if (bean != null) {
			bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
		}
		return bean;
	}

	protected Object applyBeanPostProcessorsBeforeInstantiation(Class beanClass, String beanName) {
		for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
			//如果BeanPostProcessor实现了InstantiationAwareBeanPostProcessor
			//则尝试创建beanName对应bean的代理对象，在这个方法里面还要去做切点表达式匹配，匹配上的bean才会真正创建代理对象
			if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
				Object result = ((InstantiationAwareBeanPostProcessor) beanPostProcessor).postProcessBeforeInstantiation(beanClass, beanName);
				if (result != null) {
					return result;
				}
			}
		}

		return null;
	}

	/**
	 * 核心方法，实例化一个bean
	 * @param beanName
	 * @param beanDefinition
	 * @return
	 */
	protected Object doCreateBean(String beanName, BeanDefinition beanDefinition) {
		Object bean;
		try {
			// 实例化
			bean = createBeanInstance(beanDefinition);

			// 为解决循环依赖问题，将实例化后的bean放进缓存中提前暴露
			if (beanDefinition.isSingleton()) {
				Object finalBean = bean;
				/*
					这个方法要解释一下，本质上就是调用：
						addSingletonFactory(beanName, objectFactory);
					往三级缓存加一个东西，其中：
						ObjectFactory objectFactory = new ObjectFactory<Object>(){}
					并重写了ObjectFactory#getObject方法
					这个singletonFactory就是三级缓存，但是它与singletonObjects（一级缓存）、earlySingletonObjects（二级缓存）有点区别
					一级缓存和二级缓存是Map<String, Object>类型，这个三级缓存是一个Map<String, ObjectFactory<?>>

					ObjectFactory#getObject方法实际上创建了当前bean的代理对象；如果bean的拦截器链为空，则将原对象存储在三级缓存里

					注意，这里本身不执行ObjectFactory#getObject函数，只是定义了一个ObjectFactory放入singletonFactory
					实际是在getSingleton方法中调用的（getBean方法会首先尝试调用getSingleton从缓存取bean）：
						singletonObject = singletonFactory.getObject();

					每个bean在实例化之后、属性赋值之前都会向singletonFactory里面增加一个ObjectFactory
					调用各个ObjectFactory的getObject方法返回的内容是不一样的！
					因为写死在getEarlyBeanReference方法里面的参数不同
					比方说：
						singletonFactories.get("A").getObject()
					实际执行的是：getEarlyBeanReference("A", beanDefinition_A, A)
						singletonFactories.get("B").getObject()
					实际执行的是：getEarlyBeanReference("B", beanDefinition_B, B)
				 */
				addSingletonFactory(beanName, new ObjectFactory<Object>() {
					@Override
					public Object getObject() throws BeansException {
						return getEarlyBeanReference(beanName, beanDefinition, finalBean);
					}
				});
			}

			// 实例化bean之后执行
			boolean continueWithPropertyPopulation = applyBeanPostProcessorsAfterInstantiation(beanName, bean);
			if (!continueWithPropertyPopulation) {
				// 不需要进行后续初始化步骤
				return bean;
			}
			/*
				在设置bean属性之前，允许BeanPostProcessor修改属性值，这一步实际实现了解析@Value注解
				被component-scan扫描得到的BeanDefinition实际上PropertyValues是空的，需要结合@Value注解才能为属性赋值
				对当前已经实例化的bean来说，检查其有没有属性标注了@Value，如果有，则根据.properties文件为属性直接赋值
			 */
			applyBeanPostProcessorsBeforeApplyingPropertyValues(beanName, bean, beanDefinition);
			// 为bean填充属性
			applyPropertyValues(beanName, bean, beanDefinition);
			// 执行bean的初始化方法和BeanPostProcessor的前置和后置处理方法
			bean = initializeBean(beanName, bean, beanDefinition);
		} catch (Exception e) {
			throw new BeansException("Instantiation of bean failed", e);
		}

		//注册有销毁方法的bean
		registerDisposableBeanIfNecessary(beanName, bean, beanDefinition);

		Object exposedObject = bean;
		if (beanDefinition.isSingleton()) {
			//如果有代理对象，此处获取代理对象
			exposedObject = getSingleton(beanName);
			//将单例bean放入一级缓存暴露，这样再获取单例bean就直接从缓存中拿
			addSingleton(beanName, exposedObject);
		}
		return exposedObject;
	}

	protected Object getEarlyBeanReference(String beanName, BeanDefinition beanDefinition, Object bean) {
		Object exposedObject = bean;
		for (BeanPostProcessor bp : getBeanPostProcessors()) {
			// 去找创建代理对象的那个BeanPostProcessor，即DefaultAdvisorAutoProxyCreator
			if (bp instanceof InstantiationAwareBeanPostProcessor) {
				// 执行DefaultAdvisorAutoProxyCreator#getEarlyBeanReference方法，这里首次创建了代理对象proxy并返回
				exposedObject = ((InstantiationAwareBeanPostProcessor) bp).getEarlyBeanReference(exposedObject, beanName);
				// 如果成功创建代理对象（当前bean拦截器链不为空），就返回代理对象
				if (exposedObject == null) {
					return exposedObject;
				}
			}
		}
		// 如果当前bean拦截器链为空，无需创建代理对象，就返回原对象
		return exposedObject;
	}

	/**
	 * bean实例化后执行，如果返回false，不执行后续设置属性的逻辑
	 *
	 * @param beanName
	 * @param bean
	 * @return
	 */
	private boolean applyBeanPostProcessorsAfterInstantiation(String beanName, Object bean) {
		boolean continueWithPropertyPopulation = true;
		for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
			if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
				if (!((InstantiationAwareBeanPostProcessor) beanPostProcessor).postProcessAfterInstantiation(bean, beanName)) {
					continueWithPropertyPopulation = false;
					break;
				}
			}
		}
		return continueWithPropertyPopulation;
	}

	/**
	 * 在设置bean属性之前，允许BeanPostProcessor修改属性值
	 * 实际执行AutowiredAnnotationBeanPostProcessor里面的内容
	 * 解析@Value注解，并进行属性赋值
	 *
	 * @param beanName
	 * @param bean
	 * @param beanDefinition
	 */
	protected void applyBeanPostProcessorsBeforeApplyingPropertyValues(String beanName, Object bean, BeanDefinition beanDefinition) {
		for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
			if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
				// 这里面查询了占位符的实际数值，并通过反射替换掉了占位符
				PropertyValues pvs = ((InstantiationAwareBeanPostProcessor) beanPostProcessor).postProcessPropertyValues(beanDefinition.getPropertyValues(), bean, beanName);
				/*
					如果pvs本身就是空的，那就不进行这一步
					如果pvs里面有值，就把pvs里面的值都加到BeanDefinition.PropertyValues里面
				 */
				if (pvs != null) {
					for (PropertyValue propertyValue : pvs.getPropertyValues()) {
						beanDefinition.getPropertyValues().addPropertyValue(propertyValue);
					}
				}
			}
		}
	}

	/**
	 * 注册有销毁方法的bean，即bean继承自DisposableBean或有自定义的销毁方法
	 *
	 * @param beanName
	 * @param bean
	 * @param beanDefinition
	 */
	protected void registerDisposableBeanIfNecessary(String beanName, Object bean, BeanDefinition beanDefinition) {
		/*
			只有singleton类型bean会执行销毁方法

			这是Spring的特性，如果是prototype模式，需要我们自己管理bean的生命周期
			如果是默认单例，由Spring负责创建、销毁工作
			其中创建工作由ApplicationContext在创建时实例化所有的单例bean
			销毁在这里负责
		 */
		if (beanDefinition.isSingleton()) {
			//判断bean实现了DisposableBean接口或者BeanDefinition里面有destroy-method方法
			if (bean instanceof DisposableBean || StrUtil.isNotEmpty(beanDefinition.getDestroyMethodName())) {
				registerDisposableBean(beanName, new DisposableBeanAdapter(bean, beanName, beanDefinition));
			}
		}
	}

	/**
	 * 实例化bean
	 *
	 * @param beanDefinition
	 * @return
	 */
	protected Object createBeanInstance(BeanDefinition beanDefinition) {
		return getInstantiationStrategy().instantiate(beanDefinition);
	}

	/**
	 * 为bean填充属性
	 *
	 * @param bean
	 * @param beanDefinition
	 */
	protected void applyPropertyValues(String beanName, Object bean, BeanDefinition beanDefinition) {
		try {
			for (PropertyValue propertyValue : beanDefinition.getPropertyValues().getPropertyValues()) {
				String name = propertyValue.getName();
				Object value = propertyValue.getValue();
				if (value instanceof BeanReference) {
					// beanA依赖beanB，先实例化beanB
					BeanReference beanReference = (BeanReference) value;
					value = getBean(beanReference.getBeanName());
				} else {
					//类型转换
					Class<?> sourceType = value.getClass();
					Class<?> targetType = (Class<?>) TypeUtil.getFieldType(bean.getClass(), name);
					ConversionService conversionService = getConversionService();
					if (conversionService != null) {
						if (conversionService.canConvert(sourceType, targetType)) {
							value = conversionService.convert(value, targetType);
						}
					}
				}

				//通过反射设置属性
				BeanUtil.setFieldValue(bean, name, value);
			}
		} catch (Exception ex) {
			throw new BeansException("Error setting property values for bean: " + beanName, ex);
		}
	}

	protected Object initializeBean(String beanName, Object bean, BeanDefinition beanDefinition) {
		/*
			Aware接口相关内容，如果bean实现了BeanFactoryAware接口
			则该bean需要能够自动感知自己的BeanFactory
			就是把BeanFactory作为属性值放入bean中
			那么需要获取当前bean的BeanFactory时，就直接调用该属性就可以了
		 */
		if (bean instanceof BeanFactoryAware) {
			((BeanFactoryAware) bean).setBeanFactory(this);
		}

		//执行BeanPostProcessor的前置处理
		Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);

		try {
			// 执行初始化方法
			invokeInitMethods(beanName, wrappedBean, beanDefinition);
		} catch (Throwable ex) {
			throw new BeansException("Invocation of init method of bean[" + beanName + "] failed", ex);
		}

		//执行BeanPostProcessor的后置处理
		wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
		return wrappedBean;
	}

	@Override
	public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
			throws BeansException {
		Object result = existingBean;
		// 每个 Bean 都要经过所有的 BeanPostProcessor 处理
		for (BeanPostProcessor processor : getBeanPostProcessors()) {
			Object current = processor.postProcessBeforeInitialization(result, beanName);
			if (current == null) {
				return result;
			}
			result = current;
		}
		return result;
	}

	@Override
	public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
			throws BeansException {

		Object result = existingBean;
		for (BeanPostProcessor processor : getBeanPostProcessors()) {
			Object current = processor.postProcessAfterInitialization(result, beanName);
			if (current == null) {
				return result;
			}
			result = current;
		}
		return result;
	}

	/**
	 * 执行bean的初始化方法
	 *
	 * @param beanName
	 * @param bean
	 * @param beanDefinition
	 * @throws Throwable
	 */
	protected void invokeInitMethods(String beanName, Object bean, BeanDefinition beanDefinition) throws Throwable {
		/*
			当前bean是否实现了InitializingBean接口
			如果实现了，说明当前bean中有初始化方法init-method
			初始化方法的名称根据InitializingBean接口的定义，应该为afterPropertiesSet
			直接执行afterPropertiesSet方法
		 */
		if (bean instanceof InitializingBean) {
			((InitializingBean) bean).afterPropertiesSet();
		}
		//通过反射执行init-method
		String initMethodName = beanDefinition.getInitMethodName();
		if (StrUtil.isNotEmpty(initMethodName) && !(bean instanceof InitializingBean && "afterPropertiesSet".equals(initMethodName))) {
			Method initMethod = ClassUtil.getPublicMethod(beanDefinition.getBeanClass(), initMethodName);
			if (initMethod == null) {
				throw new BeansException("Could not find an init method named '" + initMethodName + "' on bean with name '" + beanName + "'");
			}
			initMethod.invoke(bean);
		}
	}

	public InstantiationStrategy getInstantiationStrategy() {
		return instantiationStrategy;
	}

	public void setInstantiationStrategy(InstantiationStrategy instantiationStrategy) {
		this.instantiationStrategy = instantiationStrategy;
	}
}
