package org.springframework.test.expanding;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.bean.Car;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author derekyi
 * @date 2020/12/13
 */
public class PropertyPlaceholderConfigurerTest {
	/*
		读取.properties文件，后续直接使用${}就能够使用数值

		思路很简单，最开始不知道有些属性的数值，那就用占位符代替，写入BeanDefinition
		然后在bean实例化之前，使用BeanFactoryPostProcessor改变BeanDefinition里面的占位符，替换为真正的数值
		这个BeanFactoryPostProcessor就是PropertyPlaceholderConfigurer
	 */
	@Test
	public void test() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:property-placeholder-configurer.xml");

		Car car = applicationContext.getBean("car", Car.class);
		assertThat(car.getBrand()).isEqualTo("lamborghini");
	}
}
