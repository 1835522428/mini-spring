package org.springframework.test.ioc;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.bean.Car;

public class LazyInitTest {
	/*
		懒加载

		在ApplicationContext初始化时，如果bean为懒加载模式，则不会立即被Spring IoC容器创建
		见refresh方法中调用finishBeanFactoryInitialization，再调用preInstantiateSingletons
		--------------------------------------------------------------------------------
			// 只有当bean是单例且不为懒加载才会被创建（即默认饿汉式加载），创建之后放入一级缓存便于后面取用
			if (beanDefinition.isSingleton() && !beanDefinition.isLazyInit()) {
				getBean(beanName);
			}
		--------------------------------------------------------------------------------
	 */
	@Test
	public void testLazyInit() throws InterruptedException {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:lazy-test.xml");
		System.out.println(System.currentTimeMillis() + ":applicationContext-over");
		TimeUnit.SECONDS.sleep(1);
		Car c = (Car) applicationContext.getBean("car");
		c.showTime();//显示bean的创建时间
	}
}
