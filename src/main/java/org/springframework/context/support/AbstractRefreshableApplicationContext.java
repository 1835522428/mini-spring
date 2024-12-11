package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * @author derekyi
 * @date 2020/11/28
 */
public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {

	//ApplicationContext底层还是一个BeanFactory
	private DefaultListableBeanFactory beanFactory;

	/**
	 * 创建beanFactory并加载BeanDefinition
	 * ApplicationContext底部其实还是DefaultListableBeanFactory
	 * @throws BeansException
	 */
	protected final void refreshBeanFactory() throws BeansException {
		DefaultListableBeanFactory beanFactory = createBeanFactory();
		/*
			通过资源加载器加载资源，并从资源中读出BeanDefinition
			调用 XmlBeanDefinitionReader.loadBeanDefinitions，加载xml文件中的BeanDefinition
			XmlBeanDefinitionReader底层是用ResourceLoader获取InputStream读取文件的
		 */
		loadBeanDefinitions(beanFactory);
		this.beanFactory = beanFactory;
	}

	/**
	 * 创建bean工厂
	 *
	 * @return
	 */
	protected DefaultListableBeanFactory createBeanFactory() {
		return new DefaultListableBeanFactory();
	}

	/**
	 * 加载BeanDefinition
	 *
	 * @param beanFactory
	 * @throws BeansException
	 */
	protected abstract void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException;

	public DefaultListableBeanFactory getBeanFactory() {
		return beanFactory;
	}
}
