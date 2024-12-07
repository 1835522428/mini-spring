package org.springframework.test.ioc;

import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.test.bean.Car;
import org.springframework.test.bean.Person;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author derekyi
 * @date 2020/11/26
 */
public class XmlFileDefineBeanTest {
	/*
		原本是通过代码写死的方式定义 BeanDefinition
		比方说在 PopulateBeanWithPropertyValuesTest 测试类里面
		写死了有一个 Person 类，里面有两个属性
		在有了资源加载器 ResourceLoader 之后，就可以加载 ClassPath 下的文件，读取 xml 文件里面的配置信息
		再根据 xml 文件的内容生成 BeanDefinition 就可以了
	 */
	@Test
	public void testXmlFile() throws Exception {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		/*
			在 PopulateBeanWithPropertyValuesTest 测试类中，这里需要手动创建 PropertyValues 和 BeanDefinition
			然后调用 beanFactory.registerBeanDefinition("person", beanDefinition) 向工厂中注册 Bean
			现在直接读取 xml 文件
		 */
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		// loadBeanDefinitions 被重载了三次，但最后都要调用 loadBeanDefinitions(Resource resource) 方法
		beanDefinitionReader.loadBeanDefinitions("classpath:spring.xml");
		// 至此，xml 文件中的所有 bean 都生成了 BeanDefinition，被注册到了 beanDefinitionMap 里面

		Person person = (Person) beanFactory.getBean("person");
		System.out.println(person);
		assertThat(person.getName()).isEqualTo("derek");
		assertThat(person.getCar().getBrand()).isEqualTo("porsche");

		Car car = (Car) beanFactory.getBean("car");
		System.out.println(car);
		assertThat(car.getBrand()).isEqualTo("porsche");
	}
}
