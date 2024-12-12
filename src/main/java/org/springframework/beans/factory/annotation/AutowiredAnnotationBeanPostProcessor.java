package org.springframework.beans.factory.annotation;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.TypeUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.core.convert.ConversionService;

import java.lang.reflect.Field;

/**
 * 处理@Autowired和@Value注解的BeanPostProcessor
 *
 * @author derekyi
 * @date 2020/12/27
 */
public class AutowiredAnnotationBeanPostProcessor implements InstantiationAwareBeanPostProcessor, BeanFactoryAware {

	private ConfigurableListableBeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	@Override
	public PropertyValues postProcessPropertyValues(PropertyValues pvs, Object bean, String beanName) throws BeansException {
		// 处理@Value注解
		Class<?> clazz = bean.getClass();
		// 反射，获取所有的公开属性
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			// 挨个检查属性上有没有Value注解
			Value valueAnnotation = field.getAnnotation(Value.class);
			if (valueAnnotation != null) {
				// 这个value是注解的值，例如@Value("${brand}")，那这里就获得"${brand}"，实际就是个占位符
				Object value = valueAnnotation.value();
				// 去.properties文件中找这个占位符对应的实际值
				value = beanFactory.resolveEmbeddedValue((String) value);

				// 类型转换
				Class<?> sourceType = value.getClass();
				Class<?> targetType = (Class<?>) TypeUtil.getType(field);
				ConversionService conversionService = beanFactory.getConversionService();
				if (conversionService != null) {
					if (conversionService.canConvert(sourceType, targetType)) {
						value = conversionService.convert(value, targetType);
					}
				}
				// 通过反射设置属性值，替换掉占位符
				BeanUtil.setFieldValue(bean, field.getName(), value);
			}
		}

		// 处理@Autowired注解
		for (Field field : fields) {
			// 检查属性上有没有Autowired注解
			Autowired autowiredAnnotation = field.getAnnotation(Autowired.class);
			if (autowiredAnnotation != null) {
				Class<?> fieldType = field.getType();
				String dependentBeanName = null;
				// 检查是不是Qualifier注解
				Qualifier qualifierAnnotation = field.getAnnotation(Qualifier.class);
				Object dependentBean = null;
				if (qualifierAnnotation != null) {
					dependentBeanName = qualifierAnnotation.value();
					dependentBean = beanFactory.getBean(dependentBeanName, fieldType);
				} else {
					// 根据fieldType获取bean
					dependentBean = beanFactory.getBean(fieldType);
				}
				// 通过反射注入
				BeanUtil.setFieldValue(bean, field.getName(), dependentBean);
			}
		}

		return pvs;
	}

	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	@Override
	public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		return true;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return null;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return null;
	}
}
