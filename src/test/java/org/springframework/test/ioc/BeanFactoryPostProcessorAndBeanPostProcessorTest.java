package org.springframework.test.ioc;

import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.test.bean.Car;
import org.springframework.test.bean.Person;
import org.springframework.test.common.CustomBeanFactoryPostProcessor;
import org.springframework.test.common.CustomerBeanPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author derekyi
 * @date 2020/11/28
 */
public class BeanFactoryPostProcessorAndBeanPostProcessorTest {
	/*
		用来测试BeanFactoryPostProcessor和BeanPostProcessor的测试类
		BeanFactoryPostProcessor允许我们在bean实例化之前修改BeanDefinition的内容
		BeanPostProcessor允许我们在bean实例化之后修改或替换bean，是实现AOP的关键

		其中BeanPostProcessor又分为bean初始化之前的前置处理 postProcessBeforeInitialization
		和bean初始化之后的后置处理 postProcessAfterInitialization

		bean的整个生命周期如下：
									读取xml文件
										|
										|
							根据xml文件注册BeanDefinition
										|
										|
					通过BeanFactoryPostProcessor修改BeanDefinition
										|
										|
									bean 实例化
										|
										|
									 填充属性值（见doCreateBean()方法）
										|
										|
				BeanPostProcessor.postProcessBeforeInitialization 前置处理
										|
										|
									bean初始化
					执行初始化方法，init和destroy方法也需要在xml文件中写明，例如：
<bean id="person" class="org.springframework.test.bean.Person" init-method="customInitMethod" destroy-method="customDestroyMethod">
					或者让类实现InitializingBean和DisposableBean接口
										|
										|
				BeanPostProcessor.postProcessAfterInitialization 后置处理
										|
										|
									   使用
		在本测试类中，是手动给BeanFactory添加的BeanFactoryPostProcessor和BeanPostProcessor
		在使用了ApplicationContext作为IoC容器之后，就可以自动识别二者了（即在xml文件中配置PostProcessor）
	 */

	@Test
	public void testBeanFactoryPostProcessor() throws Exception {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		beanDefinitionReader.loadBeanDefinitions("classpath:spring.xml");

		//在所有BeanDefintion加载完成后，但在bean实例化之前，修改BeanDefinition的属性值
		//手动创建BeanFactoryPostProcessor，并手动执行
		CustomBeanFactoryPostProcessor beanFactoryPostProcessor = new CustomBeanFactoryPostProcessor();
		beanFactoryPostProcessor.postProcessBeanFactory(beanFactory);

		Person person = (Person) beanFactory.getBean("person");
		System.out.println(person);
		//name属性在CustomBeanFactoryPostProcessor中被修改为ivy
		assertThat(person.getName()).isEqualTo("ivy");
	}

	@Test
	public void testBeanPostProcessor() throws Exception {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		beanDefinitionReader.loadBeanDefinitions("classpath:spring.xml");

		//添加bean实例化后的处理器
		// 这里是手动创建的 BeanPostProcessor，并手动添加到 BeanFactory 里面
		CustomerBeanPostProcessor customerBeanPostProcessor = new CustomerBeanPostProcessor();
		/*
			CustomerBeanPostProcessor是针对beanName为"car"的bean进行的修改，所以后面要获取的示例也是"car"，才能做测试

			在使用 getBean() 方法时会自动调用 BeanPostProcessor
			具体体现在 AbstractAutowireCapableBeanFactory.doCreateBean() 方法里面有这么一行代码：
				//执行bean的初始化方法和BeanPostProcessor的前置和后置处理方法
				bean = initializeBean(beanName, bean, beanDefinition);
			在这个 initializeBean 函数里面就执行了 BeanPostProcessor 的内容
			在初始化前后分别执行 postProcessBeforeInitialization 和 postProcessAfterInitialization
		 */
		beanFactory.addBeanPostProcessor(customerBeanPostProcessor);

		Car car = (Car) beanFactory.getBean("car");
		System.out.println(car);
		//brand属性在CustomerBeanPostProcessor中被修改为lamborghini
		assertThat(car.getBrand()).isEqualTo("lamborghini");
	}
}
