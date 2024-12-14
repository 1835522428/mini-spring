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

		那这个问题怎么解决呢？既然我想给B传入A的代理对象的引用，而不是A本身实例的引用，那我就一定要往缓存中存放proxyA而不是beanA
		但是在实例化之后，属性赋值之前创建的这个代理对象，一定是一个残缺的，它里面没有属性值
		我先把这个代理对象缓存起来，供B完成属性赋值，之后再在A的创建流程中完善这个代理对象，就没问题了

		于是，在doCreateBean方法中，bean完成实例化之后，通过缓存解决循环依赖的代码变成了这样：
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
		这个三级缓存singletonFactory里面存放了一堆ObjectFactory，用来生成bean的实例或者代理

		总体流程如下：
			1.首先getBean(A)，A完成实例化之后、属性赋值之前生成一个ObjectFactory，放到三级缓存中，
			  通过这个ObjectFactory#getObject方法能够获取A的代理对象
			  （但是此时没有生成A的代理对象，只是给三级缓存存了一个能够获取proxyA的方法）
			2.之后给A属性赋值，发现A依赖于B，那么转而去运行getBean(B)
			  首先从缓存找B，找不到，那就执行doCreateBean(B)
			3.在B实例化完成之后，属性赋值之前，生成一个ObjectFactory，放到三级缓存中，
			  通过这个ObjectFactory#getObject方法能够获取的B实例
			  （但是此时没有生成B的实例，只是给三级缓存存了一个能够获取B实例的方法）
			  之后进行B属性赋值，发现B依赖与A，转而执行getBean(A)
			4.先从缓存中找，发现三级缓存中有对应A的ObjectFactory，那就调用ObjectFactory#getObject方法（见getSingleton源码）
			  这时根据没有赋值的A实例，创建了A的代理对象（此时这个代理对象属性值全为空），并将A的代理对象放入二级缓存，删除三级缓存中A的内容
			5.将获取到的proxyA返回给B创建流程，让B完成属性赋值，最后B完成创建流程之后，将B放入一级缓存，将二级三级缓存中B的数据清空
			6.回到getBean(A)的流程，根据返回的B实例完成属性赋值（此时A实例的属性都有数值了）
			  因为proxyA（现在放在二级缓存中）的创建是依赖于A实例的（见wrapIfNecessary方法），可以理解为proxyA里面有一个指向A实例的引用
			  proxyA里面的所有属性值都是根据引用的这个A实例获得的（例如：proxyA.name = a.name），
			  所以A实例完成属性赋值之后，proxyA里面的属性也就自然有值了
			7.最后A实例创建完毕，但是不返回A实例，而是将proxyA从二级缓存搬到一级缓存，并且返回proxyA

			所以一级缓存singletonObjects存储的是完全实例化完成的bean；
			二级缓存earlySingletonObjects存储的是未完成实例化的bean；
			三级缓存singletonFactories存储的是创建bean实例或者代理对象的工厂

			那有AOP的情况下，我直接使用二级缓存不行吗？
			现在还是刚才的场景，A依赖于B，B依赖于A，同时A需要生成代理对象proxyA
			1.先执行getBean(A)，在beanA实例化之后、属性赋值之前，我提前根据beanA生成代理对象proxyA（直接提前生成代理，不搞什么ObjectFactory了）
			  然后把proxyA放入二级缓存，之后属性赋值，发现依赖于B，执行getBean(B)
			2.在B实例化之后、属性赋值之前，把beanB放入二级缓存，之后执行属性赋值，发现依赖于A，执行getBean(A)
			3.从二级缓存找到了A（找到的实际上是proxyA），那么返回给getBean(B)，完成属性赋值。此时赋值给beanB的a是代理对象，而不是原对象，是没问题的
			4.beanB创建完毕，放入一级缓存。返回getBean(A)，A完成属性赋值，并且由于proxyA是基于beanA创建的，proxyA也是有属性的
			5.最后getBean(A)流程完毕，把proxyA放入一级缓存，beanA没有放入缓存（但是proxyA是依赖于beanA的，所以beanA在引用链上，不会被垃圾回收掉）

			所以此时：
				一级缓存singletonObjects存储的是完全实例化完成的bean或者代理对象；
				二级缓存earlySingletonObjects存储的是未完成的bean实例或者bean的代理对象（如果需要代理就存代理对象，不需要就存原对象）；

			看起来也完全没有问题啊！那么为什么Spring使用了三级缓存呢，我认为原因主要是因为不想挪动生成代理对象的那部分程序！
			我刚才说的第二种方案，使用二级缓存，但是需要在bean实例化之后、属性赋值之前提前生成代理对象proxy！这样往缓存里面存入的才是proxyA而不是beanA
			那么原先是在哪里生成的呢？原先生成代理对象的DefaultAdvisorAutoProxyCreator是一个BeanPostProcessor，所以是在bean实例化以及属性赋值之后才做的！
			使用二级缓存的方案相当于是把DefaultAdvisorAutoProxyCreator跟其他的BeanPostProcessor分离开了，把DefaultAdvisorAutoProxyCreator单独放在了更前面的位置
			那这样其实对Spring的整体架构不太友好，都是BeanPostProcessor，有的在这里执行，有的在那里执行，就太乱了，最好还是把BeanPostProcessor放在一起

			也就是说，Spring不允许挪动DefaultAdvisorAutoProxyCreator这个程序的位置，那么生成proxy的程序就一定在bean实例化并且属性赋值之后才做！
			那么我上面说的使用二级缓存的方案就不成立了，因为无法提前生成proxy并放入二级缓存

			那咋办呢？Spring就提供了另一种方式，就是这个ObjectFactory，里面存了个getObject方法，允许通过ObjectFactory#getObject在bean实例化之后、属性赋值之前
			提前生成代理对象proxy，并放入二级缓存。

			嘿！那这样做和我把DefaultAdvisorAutoProxyCreator的位置前移，放到bean实例化之后、属性赋值之前有啥区别啊！！！
				还是有一点点区别的：如果把DefaultAdvisorAutoProxyCreator前移，就意味着所有代理对象都是在实例化之后、属性赋值之前创建了，不管有没有发生循环依赖问题；
				而提供的这个ObjectFactory#getObject只会在被循环依赖时调用，如果没有发生循环依赖，这个方法是永远不会被调用的；
				所以只有发生循环依赖问题时，才会通过ObjectFactory#getObject这个备用方案在属性赋值之前提前获取proxy，
				否则proxy仍然是在属性赋值之后生成，减少了受影响的proxy范围。


			-----------------------------------------------------------------------------------------------------------------------------
			所以面试官如果问，Spring如何解决的循环依赖问题？答案如下：

				总体上来说，Spring通过三级缓存的方式解决循环依赖，其中一级缓存singletonObjects存储完全实例化好的bean；
				二级缓存earlySingletonObjects存储还未完成属性赋值的bean实例；三级缓存singleFactories存储的是一系列工厂对象，
				通过这些工厂对象的getObject方法，可以提前获取到未完成属性赋值的bean实例或者bean的代理对象

				在一个bean实例化之后、属性赋值之前，提前将能生成当前bean的工厂放入三级缓存，暴露bean对象。
				这样在循环依赖的情况下，再次尝试获取当前bean实例时，就会直接根据三级缓存中工厂的getObject方法获取bean，提前生成对象的引用，
				从而避免了getBean的无限套娃

				那么我个人认为，使用二级缓存也是可以解决循环依赖问题的。

				首先如果不需要生成代理对象的场景下，我在bean实例化之后、属性赋值之前，直接把生成的bean实例放入二级缓存，
				后面如果产生循环依赖问题，可以直接从二级缓存拿到bean的引用，这是肯定不会出现getBean方法套娃问题的。

				但是在某个对象需要生成代理的情况下，在bean实例化之后、属性赋值之前，不能把已经生成的这个bean实例放入二级缓存，而应该把当前bean的代理对象放入二级缓存，
				那么此时就需要提前生成代理对象，也就是需要将生成代理对象的那个BeanPostProcessor前移（DefaultAdvisorAutoProxyCreator），
				然后将生成的代理对象存入二级缓存，后续产生循环依赖时，从二级缓存中拿到的也就是代理对象的引用。

				那么这样，通过在bean实例化之后、属性赋值之前提前生成代理对象的方式，也可以解决有代理对象时的循环依赖问题

				但是我们都知道，Spring它本身没有采用这种解决方案，我个人认为是因为：它不想将所有代理对象的创建放在 实例化之后、属性赋值之前 这个位置
				因为生成代理对象这个任务本身是交给BeanPostProcessor去做的，BeanPostProcessor设计之初的位置是放在属性赋值之后的
				如果单独将生成代理对象的这个BeanPostProcessor前移，Spring的整体结构就会变得混乱。

				所以Spring就多了一个三级缓存，并存放一系列工厂，在发生循环依赖时允许通过这些工厂的getObject方法提前创建代理对象，并放入二级缓存
				而不是对所有bean都提前创建代理对象，这样保证了整体结构的有序性。

				实际通过这种方式提前创建的代理对象也是放入二级缓存中，也是没有经过属性赋值的的代理对象，
				所以它跟 我在实例化之后、属性赋值之前直接把代理对象创建出来并放入二级缓存 本质上是一样的。

				核心都是想办法把proxyA或者beanA提前暴露进二级缓存中去。
			-----------------------------------------------------------------------------------------------------------------------------
	 */
	@Test
	public void testCircularReference() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:circular-reference-with-proxy-bean.xml");
		A a = applicationContext.getBean("a", A.class);
		B b = applicationContext.getBean("b", B.class);

		assertThat(b.getA() == a).isTrue();
	}
}
