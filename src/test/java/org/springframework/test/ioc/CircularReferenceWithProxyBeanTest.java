package org.springframework.test.ioc;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.bean.A;
import org.springframework.test.bean.B;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * @author derekyi
 * @date 2021/1/30
 */
public class CircularReferenceWithProxyBeanTest {
	/*
		解决有代理对象时的循环依赖问题（此测试类需要在Java1.8下运行）

		在本测试类的xml文件中，有两个类A和B，A依赖于B，B也依赖于A
		同时有一个Advisor，里面装着A的前置增强类，那么在createBean方法中调用doCreateBean
		会先创建A的实例，然后在doCreateBean中调用initializeBean，执行BeanPostProcessor的后置方法时
		会调用DefaultAdvisorAutoProxyCreator#postProcessAfterInitialization方法，去创建代理对象


	 */
	@Test
	public void testCircularReference() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:circular-reference-with-proxy-bean.xml");
		A a = applicationContext.getBean("a", A.class);
		B b = applicationContext.getBean("b", B.class);

		assertThat(b.getA() == a).isTrue();
	}
}
