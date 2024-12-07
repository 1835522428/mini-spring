package org.springframework.test.ioc;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.bean.Car;
import org.springframework.test.bean.Person;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author derekyi
 * @date 2020/11/28
 */
public class ApplicationContextTest {
	/**
	 * ApplicationContext与BeanFactory一样，都是Spring的IoC容器
	 * ApplicationContextT除了拥有BeanFactory的所有功能外
	 * 还支持BeanFactoryPostProcessor和BeanPostProcessor的自动识别、资源加载、单例Bean自动初始化等
	 * 之前是手动创建并运行的PostProcessor，现在希望能够把他们写入xml文件中自动识别，然后让容器自动添加
	 * 见spring.xml：
	 * 		<bean class="org.springframework.test.common.CustomBeanFactoryPostProcessor"/>
	 * 		<bean class="org.springframework.test.common.CustomerBeanPostProcessor"/>
	 * BeanFactoryPostProcessor和BeanPostProcessor各创建了一个
	 */
	@Test
	public void testApplicationContext() throws Exception {
		/*
			这一步 new ClassPathXmlApplicationContext() 相当于之前创建 BeanFactory 三步：
				DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
				XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
				beanDefinitionReader.loadBeanDefinitions("classpath:spring.xml");
			这一步同时在注册完BeanDefinition之后执行了BeanFactoryPostProcessor
			同时，ApplicationContext会在这一步直接实例化所有单例的bean，并放入一级缓存，后面再需要这些bean时就直接从缓存中获取
			所以后面 Car car = applicationContext.getBean("car", Car.class); 时会发现car是直接从缓存中获取的
		 */
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring.xml");
		/*
			在经过上面这一步之后，beanId == "person" 的BeanDefinition里面的name属性
			就已经被BeanFactoryPostProcessor改为"ivy"了，在创建person实例时，初始化的name就是"ivy"，而不是xml中的"derek"
			但是 beanId == "car" 的BeanDefinition里面的brand属性还是porsche（xml中的配置）
			在创建car实例时，doCreateBean方法会自动调用BeanPostProcessor的前置和后置方法
			（BeanPostProcessor的前置方法在属性赋值之后执行，在init方法之前执行）
			这时才会改变car实例的内容，而不是改变BeanDefinition的内容

			bean的生命周期（参考doCreateBean方法）：
				读取xml文件并注册BeanDefinition-->BeanFactoryPostProcessor修改BeanDefinition
			 -->bean实例化和属性填充-->BeanPostProcessor前置处理-->初始化（执行bean的初始化方法，但是平常写的很多bean都没有init方法和destroy方法）
			 -->BeanPostProcessor后置处理-->使用
		 */
		Person person = applicationContext.getBean("person", Person.class);
		System.out.println(person);
		//name属性在CustomBeanFactoryPostProcessor中被修改为ivy
		assertThat(person.getName()).isEqualTo("ivy");

		//在ApplicationContext.refresh()函数里面有一句finishBeanFactoryInitialization，把所有单例bean给提前实例化了
		//所以这里不会再做一遍实例化操作，而是直接从缓存中获取单例bean
		Car car = applicationContext.getBean("car", Car.class);
		System.out.println(car);
		//brand属性在CustomerBeanPostProcessor中被修改为lamborghini
		assertThat(car.getBrand()).isEqualTo("lamborghini");
	}
}
