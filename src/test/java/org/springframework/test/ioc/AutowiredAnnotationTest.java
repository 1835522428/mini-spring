package org.springframework.test.ioc;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.bean.Person;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author derekyi
 * @date 2020/12/27
 */
public class AutowiredAnnotationTest {
	/*
		在Person类中使用Autowired注入了一个Car

		Autowired采用和Value注解非常相似的处理方式，也是在AutowiredAnnotationBeanPostProcessor
		里面的postProcessPropertyValues方法中处理
		即Autowired注解也是在bean实例化之后，赋值之前用BeanPostProcessor去处理
	 */
	@Test
	public void testAutowiredAnnotation() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:autowired-annotation.xml");

		Person person = applicationContext.getBean(Person.class);
		assertThat(person.getCar()).isNotNull();
	}
}
