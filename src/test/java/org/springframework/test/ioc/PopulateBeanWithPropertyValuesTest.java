package org.springframework.test.ioc;

import org.junit.Test;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.test.bean.Car;
import org.springframework.test.bean.Person;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author derekyi
 * @date 2020/11/24
 */
public class PopulateBeanWithPropertyValuesTest {

	/**
	 * 为 bean 注入普通属性，例如 String，int
	 * @throws Exception
	 */
	@Test
	public void testPopulateBeanWithPropertyValues() throws Exception {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		PropertyValues propertyValues = new PropertyValues();
		propertyValues.addPropertyValue(new PropertyValue("name", "derek"));
		propertyValues.addPropertyValue(new PropertyValue("age", 18));
		BeanDefinition beanDefinition = new BeanDefinition(Person.class, propertyValues);
		beanFactory.registerBeanDefinition("person", beanDefinition);

		/*
			从这里其实可以看到，BeanDefinition 注册的时候，并没有进行实例化
			而是在 BeanDefinition 里面保存了类名和对应的所有属性值 PropertyValues
			当第一次使用 getBean 获取时，才会真的创建实例
			把 BeanDefinition 里面所有的属性和值取出来，赋值给实例

			这里需要结合 Spring 去理解，Spring 是需要自己写 xml 配置文件注明 bean 的，例如 spring.xml：
				<bean id="person" class="org.springframework.test.bean.Person">
					<property name="name" value="derek"/>
					<property name="car" ref="car"/>
				</bean>

				<bean id="car" class="org.springframework.test.bean.Car">
					<property name="brand" value="porsche"/>
				</bean>
			在 xml 里面定义 bean 组件时，会给 bean 两个参数：id 和 class
			就对应 beanName 和 BeanDefinition 里面的 Class 属性
			xml 文件里面的一堆 property 就对应 propertyValues
			Spring 就是把 xml 文件里面的内容读出来，通过类似本测试函数的形式，把 bean 注册到工厂里面的
			如果 property 里面的参数是 value，就直接放到 propertyValues 里面
			如果 property 里面的参数是 ref，就把 BeanReference 类型放到 propertyValues 里面
			等实例化的时候，根据程序逻辑就会先实例化 BeanReference 里面的对象

			现在相当于在代码中写死了，工厂中一共就一个 bean，名为 person，属于 Person 类，里面有 name 和 age 属性
			后面需要构建一个通用的程序，从 xml 文件中读取这些信息，动态创建 bean
			参考 Resource、ResourceLoader 接口及其实现类，转移至测试类 ResourceAndResourceLoaderTest 查看使用和实现方式
		 */
		Person person = (Person) beanFactory.getBean("person");
		System.out.println(person);
		assertThat(person.getName()).isEqualTo("derek");
		assertThat(person.getAge()).isEqualTo(18);
	}

	/**
	 * 为bean注入bean，在 Person 类里面有一个属性 car，是 Car 类型的，需要先实例化 Car
	 *
	 * @throws Exception
	 */
	@Test
	public void testPopulateBeanWithBean() throws Exception {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		//注册Car实例
		PropertyValues propertyValuesForCar = new PropertyValues();
		propertyValuesForCar.addPropertyValue(new PropertyValue("brand", "porsche"));
		BeanDefinition carBeanDefinition = new BeanDefinition(Car.class, propertyValuesForCar);
		beanFactory.registerBeanDefinition("car", carBeanDefinition);

		//注册Person实例
		PropertyValues propertyValuesForPerson = new PropertyValues();
		propertyValuesForPerson.addPropertyValue(new PropertyValue("name", "derek"));
		propertyValuesForPerson.addPropertyValue(new PropertyValue("age", 18));
		//Person实例依赖Car实例
		/*
		  BeanReference 类只是用来标注当前属性是一个类的实例，这样在实例化的时候就需要先去构造这个属性的实例
		  其他属性，例如 name age 都是直接传值，但是 Car 这个属性是一个对象，需要先实例化才能传入
		  实例化的工作也由 Spring 去做，所以需要提前把 Car 的 BeanDefinition 也放入工厂里面
		  这里并不进行 Car 实例化的工作
		  在 AbstractAutowireCapableBeanFactory.doCreateBean 方法里会调用 applyPropertyValues 为 Bean 填充属性
		  这个时候会遍历 PropertyValues，发现在遍历过程中有这么一段代码：
		  		if (value instanceof BeanReference) {
		  			// beanA依赖beanB，先实例化beanB
		  			BeanReference beanReference = (BeanReference) value;
		  			value = getBean(beanReference.getBeanName());
		  		}
		  如果这个 propertyValue 是 BeanReference 的一个实例，就说明 beanA 依赖 beanB，会先实例化 beanB
		  实例 beanB 的方法就是先调用 getBean(beanB.name)

		  这就是 Spring 实现的自动依赖管理
		 */
		propertyValuesForPerson.addPropertyValue(new PropertyValue("car", new BeanReference("car")));
		BeanDefinition beanDefinition = new BeanDefinition(Person.class, propertyValuesForPerson);
		beanFactory.registerBeanDefinition("person", beanDefinition);

		Person person = (Person) beanFactory.getBean("person");
		System.out.println(person);
		assertThat(person.getName()).isEqualTo("derek");
		assertThat(person.getAge()).isEqualTo(18);
		Car car = person.getCar();
		assertThat(car).isNotNull();
		assertThat(car.getBrand()).isEqualTo("porsche");
	}
}
