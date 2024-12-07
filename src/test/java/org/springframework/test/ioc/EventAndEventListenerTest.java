package org.springframework.test.ioc;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.common.event.CustomEvent;

/**
 * @author derekyi
 * @date 2020/12/5
 */
public class EventAndEventListenerTest {

	@Test
	public void testEventListener() throws Exception {
		/*
			容器事件和事件监听器（核心是ApplicationEventMulticaster）

			在xml文件中定义了三个事件监听器：
				ContextRefreshedEventListener、CustomEventListener、ContextClosedEventListener
			事件监听器要实现ApplicationListener接口，并在泛型中标明要监听的事件类型
			在refresh()方法中，有一个initApplicationEventMulticaster()方法用来初始化事件发布者：
				这一步创建了ApplicationEventMulticaster实例（事件发布者实例）并把它放入IoC容器的一级缓存
			接下来调用registerListeners()方法注册事件监听器：
				会把所有实现了ApplicationListener的类都注册到ApplicationEventMulticaster实例中
				存放在ApplicationEventMulticaster.applicationListeners（一个Set）里面
			当refresh()方法执行到最后一行，有一个finishRefresh()方法，这个方法发布了一个“容器刷新完成”事件：
				发布事件调用的是ApplicationEventMulticaster.multicastEvent方法
				这个方法会遍历ApplicationEventMulticaster.applicationListeners里面存放的所有事件监听器
				看看有没有监听器需要对此事件作出反应，并执行对应的函数
				这里会触发ContextRefreshedEventListener
			接下来在当前测试类下面手动调用publishEvent方法，产生一个自定义的CustomEvent事件
				这里会触发CustomEventListener
			最后调用关闭容器方法，触发ContextClosedEventListener
		 */
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:event-and-event-listener.xml");
		applicationContext.publishEvent(new CustomEvent(applicationContext));

		applicationContext.registerShutdownHook();//或者applicationContext.close()主动关闭容器;
	}
}
