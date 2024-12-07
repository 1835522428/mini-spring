package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Collection;
import java.util.Map;

/**
 * 抽象应用上下文
 *
 * @author derekyi
 * @date 2020/11/28
 */
public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {

	public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";

	public static final String CONVERSION_SERVICE_BEAN_NAME = "conversionService";

	private ApplicationEventMulticaster applicationEventMulticaster;

	@Override
	public void refresh() throws BeansException {
		//创建BeanFactory，并加载BeanDefinition
		refreshBeanFactory();
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();

		//添加ApplicationContextAwareProcessor，让继承自ApplicationContextAware的bean能感知bean
		//Aware 接口相关内容
		beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));

		/*
			在bean实例化之前，执行BeanFactoryPostProcessor

			重温bean的生命周期：
				读取xml文件->生成BeanDefinition->BeanFactoryPostProcessor
			  ->实例化->BeanPostProcessor前置处理->初始化->BeanPostProcessor后置处理->使用

			由于BeanFactoryPostProcessor也是要作为bean写在xml文件里面的
			所以它应该跟其他bean一起被注册到了BeanDefinitionMap里面
			下面这个函数就是去把BeanDefinitionMap里面所有的BeanFactoryPostProcessor运行一遍
		 */
		invokeBeanFactoryPostProcessors(beanFactory);

		/*
			BeanPostProcessor需要提前于其他bean实例化之前注册

			这里只是调用beanFactory.addBeanPostProcessor
			把各个BeanPostProcessor（实例）放到BeanFactory里面，没有执行
			在调用BeanFactory.getBean实例化某个Bean时，进行初始化赋值操作前后会自动调用BeanPostProcessor
			详解见测试类BeanFactoryPostProcessorAndBeanPostProcessorTest
		 */
		registerBeanPostProcessors(beanFactory);

		//初始化事件发布者
		initApplicationEventMulticaster();

		//注册事件监听器
		registerListeners();

		/*
			注册类型转换器和提前实例化单例bean

			这里对所有单例bean进行了实例化，所以再次getBean的时候就直接从缓存里面取了
			之前在BeanFactory里面没有这一步，bean在第一次getBean时实例化
			ApplicationContext里面实现了单例bean的自动实例化，在创建ApplicationContext是就做了实例化操作
		 */
		finishBeanFactoryInitialization(beanFactory);

		//发布容器刷新完成事件
		finishRefresh();
	}

	protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
		//设置类型转换器
		if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME)) {
			Object conversionService = beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME);
			if (conversionService instanceof ConversionService) {
				beanFactory.setConversionService((ConversionService) conversionService);
			}
		}

		//提前实例化单例bean
		beanFactory.preInstantiateSingletons();
	}

	/**
	 * 创建BeanFactory，并加载BeanDefinition
	 *
	 * @throws BeansException
	 */
	protected abstract void refreshBeanFactory() throws BeansException;

	/**
	 * 在bean实例化之前，执行BeanFactoryPostProcessor
	 *
	 * @param beanFactory
	 */
	protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		//首先获取所有的BeanFactoryPostProcessor，遍历BeanDefinitionMap，返回所有实现了BeanFactoryPostProcessor类的实例
		Map<String, BeanFactoryPostProcessor> beanFactoryPostProcessorMap = beanFactory.getBeansOfType(BeanFactoryPostProcessor.class);
		for (BeanFactoryPostProcessor beanFactoryPostProcessor : beanFactoryPostProcessorMap.values()) {
			//执行所有的BeanFactoryPostProcessor
			beanFactoryPostProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * 注册BeanPostProcessor
	 *
	 * @param beanFactory
	 */
	protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		Map<String, BeanPostProcessor> beanPostProcessorMap = beanFactory.getBeansOfType(BeanPostProcessor.class);
		for (BeanPostProcessor beanPostProcessor : beanPostProcessorMap.values()) {
			beanFactory.addBeanPostProcessor(beanPostProcessor);
		}
	}

	/**
	 * 初始化事件发布者
	 */
	protected void initApplicationEventMulticaster() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
		beanFactory.addSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, applicationEventMulticaster);
	}

	/**
	 * 注册事件监听器
	 */
	protected void registerListeners() {
		Collection<ApplicationListener> applicationListeners = getBeansOfType(ApplicationListener.class).values();
		for (ApplicationListener applicationListener : applicationListeners) {
			applicationEventMulticaster.addApplicationListener(applicationListener);
		}
	}

	/**
	 * 发布容器刷新完成事件
	 */
	protected void finishRefresh() {
		publishEvent(new ContextRefreshedEvent(this));
	}

	@Override
	public void publishEvent(ApplicationEvent event) {
		applicationEventMulticaster.multicastEvent(event);
	}

	@Override
	public boolean containsBean(String name) {
		return getBeanFactory().containsBean(name);
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		return getBeanFactory().getBean(name, requiredType);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
		return getBeanFactory().getBeansOfType(type);
	}

	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return getBeanFactory().getBean(requiredType);
	}

	@Override
	public Object getBean(String name) throws BeansException {
		return getBeanFactory().getBean(name);
	}

	public String[] getBeanDefinitionNames() {
		return getBeanFactory().getBeanDefinitionNames();
	}

	public abstract ConfigurableListableBeanFactory getBeanFactory();

	public void close() {
		doClose();
	}

	public void registerShutdownHook() {
		Thread shutdownHook = new Thread() {
			public void run() {
				doClose();
			}
		};
		Runtime.getRuntime().addShutdownHook(shutdownHook);

	}

	protected void doClose() {
		//发布容器关闭事件
		publishEvent(new ContextClosedEvent(this));

		//执行单例bean的销毁方法
		destroyBeans();
	}

	protected void destroyBeans() {
		getBeanFactory().destroySingletons();
	}
}

