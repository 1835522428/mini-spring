package org.springframework.context.support;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * @author derekyi
 * @date 2020/11/28
 */
public abstract class AbstractXmlApplicationContext extends AbstractRefreshableApplicationContext {

	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory, this);
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			// 这个loadBeanDefinitions就是那个重载了三次的方法，最后都要去调用loadBeanDefinitions(Resource resource)
			beanDefinitionReader.loadBeanDefinitions(configLocations);
		}
	}

	protected abstract String[] getConfigLocations();
}
