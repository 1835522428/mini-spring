package org.springframework.test.ioc;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.bean.A;
import org.springframework.test.bean.B;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * @author derekyi
 * @date 2021/1/25
 */
public class CircularReferenceWithoutProxyBeanTest {
	/*
		Spring解决循环依赖问题，见xml文件定义了两个bean，其中a依赖b，b依赖a
		如果此时不做任何处理，将发生以下问题：
			先实例化A，发现A依赖于B，所以转而去实例化B
			实例化B，发现B依赖于A，所以转而去实例化A
			实例化A，发现A依赖于B，所以转而去实例化B
			...
		死循环直至栈溢出
		Spring使用三级缓存解决循环依赖问题
		见refresh中调用实例化单例bean的方法finishBeanFactoryInitialization
		在这里最终调用了doCreateBean方法，在该方法中，实例化完成之后，属性赋值之前有以下定义（简化版本）：
		------------------------------------------------------------------------------------
			// 实例化
			bean = createBeanInstance(beanDefinition);

			// 为解决循环依赖问题，将实例化后的bean放进缓存中提前暴露
			if (beanDefinition.isSingleton()) {
				// earlySingleObjects是二级缓存，singleObjects是一级缓存
				earlySingleObjects.put(beanName, bean);
			}

			// 为bean填充属性
			applyPropertyValues(beanName, bean, beanDefinition);
		------------------------------------------------------------------------------------
		之后进入填充属性的方法，在该方法中，检测如果某个属性是BeanReference类型，就会先去实例化BeanReference.getBeanName()
		比方说先实例化的A，然后将A放入二级缓存，为A属性填充时发现依赖于B，那么转而实例化B
		getBean(B)同样先实例化B，然后将B存入二级缓存，之后执行B的属性填充，发现B依赖于A
		这时就由B调用了getBean(A)，但是这时出现了很有意思的地方，使用getBean是会先去检查缓存的！
		见getBean方法的第一行：
		------------------------------------------------------------------------------------
			//去缓存中找 bean，第一次执行的时候都是找不到的
			Object sharedInstance = getSingleton(name);
		------------------------------------------------------------------------------------
		在getSingleton中，先在一级缓存singleObjects中找有没有A的实例，发现没有
		然后去二级缓存earlySingleObjects里找有没有A的实例，发现找到了（此时A还没有完成属性赋值）
		就会返回A实例
		那么这样就打破了循环，尽管返回给B的A实例还没有完成属性赋值，但是已经分配内存地址了，可以直接赋值给B里面的属性
		这样B的实例就创建完毕（B实例创建也经过相同的步骤，在实例化之后属性赋值之前放到二级缓存，在完成创建之后放到一级缓存）
		B实例创建完成之后，回到A实例创建，那么A通过getBean(B)拿到B的实例之后，也完成属性赋值，创建完成的A实例也放到一级缓存中
		所以：
			一级缓存是为了防止单例模式下一个bean被多次创建的，加快getBean的速度
			二级缓存是为了解决循环依赖问题的，在bean实例化之后，属性赋值之前将不是完全体的bean放入二级缓存，从而防止循环依赖
		那这个三级缓存是干嘛的呢，答案是为了解决有代理对象时的循环依赖问题，见CircularReferenceWithProxyBeanTest测试类
	 */
	@Test
	public void testCircularReference() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:circular-reference-without-proxy-bean.xml");
		A a = applicationContext.getBean("a", A.class);
		B b = applicationContext.getBean("b", B.class);
		assertThat(a.getB() == b).isTrue();
	}
}
