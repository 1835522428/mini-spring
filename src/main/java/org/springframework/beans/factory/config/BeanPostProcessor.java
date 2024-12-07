package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;

/**
 * 用于修改实例化后的bean的修改扩展点
 *
 * @author derekyi
 * @date 2020/11/28
 */
public interface BeanPostProcessor {

	/**
	 * 在bean执行初始化方法之前执行此方法
	 * BeanPostProcessor 分两个功能：前置处理和后置处理
	 * 前置处理是在 bean 初始化之前进行的
	 * 后置处理是在 bean 初始化之后进行的
	 *
	 * @param bean
	 * @param beanName
	 * @return
	 * @throws BeansException
	 */
	Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException;

	/**
	 * 在bean执行初始化方法之后执行此方法
	 *
	 * @param bean
	 * @param beanName
	 * @return
	 * @throws BeansException
	 */
	Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException;
}
