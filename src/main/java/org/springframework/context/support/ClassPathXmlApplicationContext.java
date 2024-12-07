package org.springframework.context.support;

import org.springframework.beans.BeansException;

/**
 * xml文件的应用上下文
 *
 * @author derekyi
 * @date 2020/11/28
 */
public class ClassPathXmlApplicationContext extends AbstractXmlApplicationContext {

	private String[] configLocations;

	/**
	 * 从xml文件加载BeanDefinition，并且自动刷新上下文
	 *
	 * @param configLocation xml配置文件
	 * @throws BeansException 应用上下文创建失败
	 */
	public ClassPathXmlApplicationContext(String configLocation) throws BeansException {
		this(new String[]{configLocation});
	}

	/**
	 * 从xml文件加载BeanDefinition，并且自动刷新上下文
	 *
	 * @param configLocations xml配置文件
	 * @throws BeansException 应用上下文创建失败
	 */
	public ClassPathXmlApplicationContext(String[] configLocations) throws BeansException {
		//configLocations 里面都是 xml 配置类的路径
		this.configLocations = configLocations;
		//ApplicationContext 实现自动识别 PostProcessor 的核心方法
		refresh();
	}

	protected String[] getConfigLocations() {
		return this.configLocations;
	}
}
