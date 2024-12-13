package org.springframework.test.ioc;

import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
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
		同时有一个Advisor，里面装着A的前置增强类（增强A#func方法），那么在createBean方法中调用doCreateBean
		会先创建A的实例并属性赋值，然后在doCreateBean中调用initializeBean，执行BeanPostProcessor的后置方法时
		会调用DefaultAdvisorAutoProxyCreator.postProcessAfterInitialization方法，去创建代理对象
		（这里得区分一下，代理对象是在bean实例化并且属性赋值之后通过BeanPostProcessor后置处理创建的；
		  Value和Autowired注解是在bean实例化之后、属性赋值之前通过BeanPostProcessor做的解析；
		  解决循环依赖是在实例化之后、属性赋值之前将bean提前放进二级缓存）

		现在假设我们仍然按照二级缓存的思路来解决这个循环依赖问题：
			先getBean(A)，在doCreateBean中，将A实例化之后，放入二级缓存，之后为A属性赋值发现A依赖于B，执行getBean(B)；
			实例化B，并将B提前放入二级缓存，为B进行属性填充，发现B依赖于A，去执行getBean(A)；
			先从缓存中找A实例，发现二级缓存中有，那就直接返回给B，让B完成实例化，返回B实例	---> 这里出了问题！！！
			B完成实例化之后返回到创建A的流程，A属性填充完毕，通过BeanPostProcessor创建代理对象，返回A的代理对象
		乍一看好像没什么问题，A的代理对象和B的实例都成功创建了，但是有个巨大的BUG，传到B实例里面的那个A，不是A的代理对象，而是A的实例！
		所以如果调用B.a的话，会发现我们的代理好像根本没有生效；但是直接调用A，好像代理又生效了

		那这个问题怎么解决呢？既然我想给B传入A的代理对象的引用，而不是A本身实例的引用，那我就一定要先把A的代理对象创建出来
		但是在实例化之后，属性赋值之前创建的这个代理对象，一定是一个残缺的，它里面没有属性值
		我先把这个代理对象缓存起来，供B完成属性赋值，之后再在A的创建流程中完善这个代理对象，就没问题了

		于是，在doCreateBean方法中，bean完成实例化之后通过缓存解决循环依赖的代码变成了这样：
		-------------------------------------------------------------------------------------------------------
			// 为解决循环依赖问题，将实例化后的bean放进缓存中提前暴露
			if (beanDefinition.isSingleton()) {
				Object finalBean = bean;
				addSingletonFactory(beanName, new ObjectFactory<Object>() {
					@Override
					public Object getObject() throws BeansException {
						return getEarlyBeanReference(beanName, beanDefinition, finalBean);
					}
				});
			}
		-------------------------------------------------------------------------------------------------------

	 */
	@Test
	public void testCircularReference() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:circular-reference-with-proxy-bean.xml");
		A a = applicationContext.getBean("a", A.class);
		B b = applicationContext.getBean("b", B.class);

		assertThat(b.getA() == a).isTrue();
	}
}
