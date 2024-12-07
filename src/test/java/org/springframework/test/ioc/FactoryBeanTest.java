package org.springframework.test.ioc;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.bean.Car;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author derekyi
 * @date 2020/12/2
 */
public class FactoryBeanTest {

	@Test
	public void testFactoryBean() throws Exception {
		/*
			这里去读取classpath:factory-bean.xml文件，读出来的BeanDefinition信息是：beanId = "car"; class = CarFactoryBean;（默认单例）
			在ApplicationContext创建时，会在doCreateBean()中实例化这个单例Bean，这个时候实例化得到的是CarFactoryBean的实例，而不是Car的实例
			并且为CarFactoryBean实例的属性赋值了，赋值的内容也和xml对应：<property name="brand" value="porsche"/> 将brand赋值为"porsche"
			所以如果去查看一级缓存的话，应该能发现有一个CarFactoryBean的实例
			doCreateBean()返回之后，程序会回到AbstractBeanFactory.getBean()方法，getBean()方法里面最后两行是这么写的：
			-----------------------------------------------------------------------------------------------------------------------
				Object bean = createBean(name, beanDefinition);	//调用doCreateBean，实例化bean并属性赋值，返回的是CarFactoryBean实例
				return getObjectForBeanInstance(bean, name);	//关键点在这里！！！
			-----------------------------------------------------------------------------------------------------------------------
			getBean()方法没有直接返回bean，而是调用了getObjectForBeanInstance()方法
			这个方法会判断当前的bean是不是实现了FactoryBean接口，如果没有实现，直接返回bean；如果实现了，会做以下处理：（精简之后的代码）
			-----------------------------------------------------------------------------------------------------------------------
				Object object = beanInstance;								//默认返回的object就是传入的参数beanInstance
				if (beanInstance instanceof FactoryBean) {					//如果当前的beanInstance实现了FactoryBean
					FactoryBean factoryBean = (FactoryBean) beanInstance;
					object = factoryBean.getObject();						//调用FactoryBean接口的getObject方法，并覆盖原返回值
					this.factoryBeanObjectCache.put(beanName, object);		//将新生成的object放到一个专门的factoryBeanObjectCache缓存中
				}
				return object;												//返回值为传入的参数beanInstance或者factoryBean.getObject()
			-----------------------------------------------------------------------------------------------------------------------
			CarFactoryBean实现了FactoryBean接口，因此会调用其getObject()方法，而这个getObject()方法是这样写的：
			-----------------------------------------------------------------------------------------------------------------------
				public Car getObject() throws Exception {
					Car car = new Car();	//生成了一个Car的实例
					car.setBrand(brand);	//对Car实例做了一些操作
					return car;				//返回Car实例
				}
			-----------------------------------------------------------------------------------------------------------------------
			这样这个Car实例就会覆盖掉CarFactoryBean实例，返回给函数。并且这个Car实例还会被加入到factoryBeanObjectCache里面缓存
			注意，这个Car实例在一级缓存singletonObjects里面是没有的：
				一级缓存singletonObjects里面存放的是：beanId = car --> CarFactoryBean实例
				factoryBeanObjectCache里面缓存的是：beanId = car --> Car实例
			这样再次调用applicationContext.getBean()获取"car"这个bean时，从缓存中首先获取到的是CarFactoryBean实例
			这个时候也不是直接返回，还是去调用了getObjectForBeanInstance()方法！！！（第27行提到过的方法！！！）
			-----------------------------------------------------------------------------------------------------------------------
				Object sharedInstance = getSingleton(name);					//去缓存中找bean，找到的是CarFactoryBean实例
				if (sharedInstance != null) {
					//如果是FactoryBean，从FactoryBean#getObject中创建bean
					return getObjectForBeanInstance(sharedInstance, name);	//如果当前类实现了FactoryBean接口
																			//就去factoryBeanObjectCache缓存里查找真正的实例
																			//最后找到的是Car实例
				}
			-----------------------------------------------------------------------------------------------------------------------
			这样最后还是返回的Car实例
			所以可以理解为CarFactoryBean只是一个中间的工具，最开始是实例化CarFactoryBean，但是由于它是FactoryBean的实现类，会调用其getObject()方法
			在getObject()方法里面，用户可以自定义某一个类的创建流程，比方说某些实例的创建非常复杂，需要传固定的参数，就可以在getObject()方法里完成
			这样用户最终可以获得getObject()返回的实例。

			Spring默认的实例化方式可以参考doCreateBean里面的内容，先通过反射的方式创建实例，然后为bean填充属性值，再执行init方法，就得到了最后的bean
			有些bean的创建不能用简单的实例化+属性赋值方式完成（个人理解，例如属性的参数不确定），那就可以通过FactoryBean跳过Spring的默认实例化方式
			具体使用方式分为两步：写一个新的类实现FactoryBean接口；在getObject()方法里面定义bean的实例化方法

			说起来有点好玩，在xml文件中定义的明明是CarFactoryBean，但是返回的却是Car实例，用户实际拿不到CarFactoryBean实例
			在表面上没有实例化Car，却可以通过FactoryBean.getObject()创建Car实例

			所以这里需要记住一个原则：FactoryBean类型的实例永远不会被程序员拿到，程序员拿到的都是FactoryBean.getObject()返回的实例
			我们根本不关心FactoryBean实例，我们访问FactoryBean实例都是为了去获取getObject()里面的东西
			而FactoryBean和目标实例在各自的缓存中的beanId是一样的，所以程序员可以把他们俩看作一体的，但是对于Spring来说这是两个缓存里面的两个实例
			我们通过"car"这个beanId原本应该拿到CarFactoryBean实例，但是Car实例把他给顶替掉了
			通过CarFactoryBean的beanId拿到的是CarFactoryBean.getObject()返回的实例
		 */
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:factory-bean.xml");

		Car car = applicationContext.getBean("car", Car.class);
		assertThat(car.getBrand()).isEqualTo("porsche");
	}
}
