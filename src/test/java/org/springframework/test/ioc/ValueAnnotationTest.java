package org.springframework.test.ioc;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.bean.Car;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author derekyi
 * @date 2020/12/27
 */
public class ValueAnnotationTest {
	/*
		@Value注解设置bean的属性值

		在@Component注解那里提到过，Component只能扫描得知哪些类要被声明为bean，不会扫描里面的属性
		生成的BeanDefinition里只有beanId和Class，而PropertyValues里面是空的（ClassPathBeanDefinitionScanner#doScan方法）

		在classpath:value-annotation.xml文件中定义了一个bean，一个component-scan
		在doLoadBeanDefinitions方法中，会读取xml文件，读取到bean的定义时，根据property的设置生成BeanDefinition
		读取到component-scan时，会扫描base-package路径下的所有类，对于带有@Component注解的类，生成BeanDefinition，但是并不考虑PropertyValues属性
		在本例中，扫描得到了一个名为car的bean和一个名为person的bean

		那实例化的时候没有属性值怎么办呢？
			思路1：使用 @Value注解 + BeanFactoryPostProcessor 在实例化之前修改BeanDefinition，填充属性值
			思路2（Spring实际使用的方式）：使用 @Value注解 + BeanPostProcessor 在bean实例化之后，属性赋值之前检查当前实例里面的属性有没有带Value注解的，
			如果有，则使用提前注册好的解析器，将注解的属性根据.properties里面的内容赋值
		这个BeanPostProcessor就是AutowiredAnnotationBeanPostProcessor，具体实现方法为postProcessPropertyValues

		那么这里有个问题，在xml文件中没有定义这个BeanPostProcessor啊，那他是怎么被注册为bean的呢？
		见ClassPathBeanDefinitionScanner#doScan方法，在执行component-scan时，最后一行自动把这个BeanPostProcessor注册进去了
		（所以印证了之前的猜测，component-scan必须和Value注解一起使用，要不然没法属性赋值，所以一旦调用了component-scan，就会自动注册Value解析器）
		在refresh函数中调用registerBeanPostProcessors时，就会生成所有BeanPostProcessor实例，并放到BeanFactory里面暂存
		接下来refresh方法继续向后执行

		在refresh方法中的finishBeanFactoryInitialization方法，开始实例化单例bean。那么按之前的理论，在bean实例化之后，赋值之前解析@Value注解应该就在这里面实现
		这里面最终调用了doCreateBean方法，在doCreateBean方法中，有这么三行：
		------------------------------------------------------------------------------------------------------------------------------
			//在设置bean属性之前，允许BeanPostProcessor修改属性值
			applyBeanPostProcessorsBeforeApplyingPropertyValues(beanName, bean, beanDefinition);
			//为bean填充属性
			applyPropertyValues(beanName, bean, beanDefinition);
			//执行bean的初始化方法和BeanPostProcessor的前置和后置处理方法
			bean = initializeBean(beanName, bean, beanDefinition);
		------------------------------------------------------------------------------------------------------------------------------
		那么在填充属性之前，也就是applyBeanPostProcessorsBeforeApplyingPropertyValues这个方法里面，就是解析Value的逻辑
		在该函数中，首先获取实现了InstantiationAwareBeanPostProcessor接口的BeanPostProcessor
		实际也就是获取了AutowiredAnnotationBeanPostProcessor，之后执行了它的postProcessPropertyValues方法
		在这里完成了占位符替换为实际值的操作，直接为bean中有Value注解的属性完成了赋值
	 */
	@Test
	public void testValueAnnotation() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:value-annotation.xml");

		Car car = applicationContext.getBean("car", Car.class);
		assertThat(car.getBrand()).isEqualTo("lamborghini");
	}
}
