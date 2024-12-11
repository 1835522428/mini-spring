package org.springframework.test.ioc;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.bean.Car;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * @author derekyi
 * @date 2020/12/26
 */
public class PackageScanTest {
	/*
		包扫描测试类

		要想进行包扫描，需要现声明好Component-scan，可以在xml中声明，参考classpath:package-scan.xml
		也可以使用注解配置，例如SpringBoot启动类上面的@SpringBootApplication注解，该注解包含了@ComponentScan注解
		声明了扫描范围之后，Spring会自动把范围内所有带@Component、@Service等注解的类注册为bean

		具体这个component-scan是在哪里使用的呢，在bean生命周期的第一步，读取xml配置文件里面就用到了
		见doLoadBeanDefinitions方法，在该方法中最终调用了scanPackage方法，扫描注解Component的类，提取信息，组装成BeanDefinition
		最后自动放到BeanDefinitionMap里面
		但是这种自动创建的BeanDefinition本身PropertyValues是空的，因为扫描的时候只会得知哪些类上面有Component注解，需要注册为bean
		但是不会去看这个类里面都有哪些属性，所以实际的BeanDefinition里面只有beanId和Class
		那么这种情况下属性赋值是怎么做的呢？答案是通过 @Value注解 + BeanFactoryPostProcessor 修改BeanDefinition
	 */
	@Test
	public void testScanPackage() throws Exception {
		/*
			在这个xml文件中并没哟显式地注册任何的bean，只声明了一个component-scan，扫描范围是test/bean文件夹
			在该文件夹中有四个类，其中A和B没有添加注解，Car和Person添加了@Component注解
		 */
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:package-scan.xml");

		Car car = applicationContext.getBean("car", Car.class);
		assertThat(car).isNotNull();
	}
}
