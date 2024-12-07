package org.springframework.context.event;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author derekyi
 * @date 2020/12/5
 */
public class SimpleApplicationEventMulticaster extends AbstractApplicationEventMulticaster {

	public SimpleApplicationEventMulticaster(BeanFactory beanFactory) {
		setBeanFactory(beanFactory);
	}

	@Override
	public void multicastEvent(ApplicationEvent event) {
		for (ApplicationListener<ApplicationEvent> applicationListener : applicationListeners) {
			if (supportsEvent(applicationListener, event)) {
				applicationListener.onApplicationEvent(event);
			}
		}
	}

	/**
	 * 监听器是否对该事件感兴趣
	 *
	 * @param applicationListener
	 * @param event
	 * @return
	 */
	protected boolean supportsEvent(ApplicationListener<ApplicationEvent> applicationListener, ApplicationEvent event) {
		/*
			获取事件监听器所实现的接口，一个事件监听器的格式如下：
				public class ContextRefreshedEventListener implements ApplicationListener<ContextRefreshedEvent> {
			实现的接口应该是ApplicationListener<E>
			[0]表示获取实现的第一个接口，这里实际上就实现了一个接口（一个类是可以实现多个接口的）
		 */
		Type type = applicationListener.getClass().getGenericInterfaces()[0];
		//获取监听器实现的第一个泛型
		Type actualTypeArgument = ((ParameterizedType) type).getActualTypeArguments()[0];
		String className = actualTypeArgument.getTypeName();
		Class<?> eventClassName;
		try {
			//获取监听器泛型的那个类
			eventClassName = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new BeansException("wrong event class name: " + className);
		}
		//判断当前事件监听器监听的是否是当前事件
		return eventClassName.isAssignableFrom(event.getClass());
	}
}
