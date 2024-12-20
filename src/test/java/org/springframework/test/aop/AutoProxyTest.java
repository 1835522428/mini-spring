package org.springframework.test.aop;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.service.WorldService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author derekyi
 * @date 2020/12/6
 */
public class AutoProxyTest {
	/*
		在createBean方法中是这么写的：
		-----------------------------------------------------------------------------------------------------------
			protected Object createBean(String beanName, BeanDefinition beanDefinition) throws BeansException {
				//如果bean需要代理，则直接返回代理对象
				//resolveBeforeInstantiation这个方法直接尝试生成bean的代理对象
				//如果没有和当前bean匹配的Advisor，就说明当前bean不需要增强，不需要代理对象，直接返回null
				Object bean = resolveBeforeInstantiation(beanName, beanDefinition);
				if (bean != null) {
					return bean;
				}
				//bean不需要代理，则直接创建bean实例
				return doCreateBean(beanName, beanDefinition);
			}
		-----------------------------------------------------------------------------------------------------------
		在resolveBeforeInstantiation方法里，会把所有实现了InstantiationAwareBeanPostProcessor接口的BeanPostProcessor拿出来，执行里面的wrapIfNecessary方法

		InstantiationAwareBeanPostProcessor是一种特殊的BeanPostProcessor，该接口用于生成动态代理对象，它本身没有属性，只是实现了生成代理对象的逻辑
		其实现类DefaultAdvisorAutoProxyCreator也被放到了xml文件里，在生成ApplicationContext时，会把这个BeanPostProcessor读入
		DefaultAdvisorAutoProxyCreator里面实现了生成动态代理对象的逻辑
		见DefaultAdvisorAutoProxyCreator#wrapIfNecessary(Object bean, String beanName)方法：
			传入参数：当前尝试生成代理对象的bean和beanName
			1.读取xml中所有的PointcutAdvisor
			2.挨个检查这些Advisor的切点表达式是否匹配当前类
			3.如果匹配切点表达式，就把Advisor的MethodInterceptor加入到当前bean的拦截器链中：
				proxyFactory.addAdvisor(advisor);
			4.生成动态代理对象并返回，后续调用代理对象的方法时，代理对象会自动按照拦截器链上的顺序执行
				对于前置增强方法：
					this.advice.before();					//执行前置增强方法
					return method.proceed()					//方法继续执行
				对于后置增强方法：
					method.proceed()						//方法继续执行
					return this.advice.afterReturning();	//执行后置增强方法

			这里需要注意一点，拦截器链其实就是一个集合，里面存放了当前对象所有需要执行的MethodInterceptor（List<Advisor> advisors = new ArrayList<>();）
			但是这个集合本身是无序的，可能先存放的是后置增强方法，再存放前置增强方法
			执行的时候就按照集合顺序从前到后执行，那么先执行到后置增强方法怎么办呢？
			其实无所谓，因为就算先进入到后置增强方法，也是先执行method.proceed()，让方法继续运行，而不是直接执行afterReturning方法
			当方法继续执行，就会回到拦截器链的集合，继续执行集合后面的方法拦截器，那么这时候执行了前置增强方法
			前置增强方法的第一行是this.advice.before();直接执行增强方法，之后执行return method.proceed()，继续回到拦截器链
			直到执行完毕所有的前置增强和原方法之后，才会回到后置增强方法的return this.advice.afterReturning();（有点像递归的意思）
			这个时候才会执行后置增强方法，所以实际执行的方法拦截器本身不会乱序

		生成代理对象proxy返回到createBean方法中后，doCreateBean会被短路，直接返回代理对象proxy
		（bug fix中已经更改实现方法，doCreateBean不会被短路，resolveBeforeInstantiation方法总是返回null，proxy不再在此方法里生成
		所以无论怎样都会运行doCreateBean方法，生成被代理对象的bean实例，并为其属性赋值，在init方法之后的BeanPostProcessor后置处理中
		才会根据已经实例化的bean生成动态代理对象proxy并返回）

		这里需要注意一点，既然说InstantiationAwareBeanPostProcessor是特殊的BeanPostProcessor，那么它必定实现了BeanPostProcessor接口
		每个Bean在实例化完成之后，需要经过BeanPostProcessor前置处理，init方法，BeanPostProcessor后置处理
		那么这个InstantiationAwareBeanPostProcessor的前置处理做了啥呢：
			return bean;
		没错，他直接返回了，啥也不干，因为InstantiationAwareBeanPostProcessor只是用来生成动态代理对象的，它不履行实际BeanPostProcessor的职责


		所以Spring中到底怎么实现的方法增强（面向切面编程AOP）呢：
			1.既然想要使用方法增强，那么就需要实现增强方法（可以分为前置、后置等），并指明切点在那里（切点表达式）
				在Spring中，增强方法MethodInterceptor和切点表达式统一实现在PointcutAdvisor接口里
				在xml中要写明所有的PointcutAdvisor
				这一步就是平时使用Spring AOP的步骤（实现PointcutAdvisor接口，写入xml文件）
			2.在Spring中有一个特殊的BeanPostProcessor，名为InstantiationAwareBeanPostProcessor，
			  在Spring通过BeanFactory#getBean方法时，如果bean实例不存在，会调用createBean方法
			  在createBean中，会挨个检查所有的Advisor，看当前bean是否匹配切点表达式
			  如果匹配，就把当前Advisor加入到bean的拦截器链上（proxyFactory.addAdvisor(advisor);），
			  遍历完所有的Advisor之后，使用JDK或者CGLIB动态代理，创建代理对象，代理对象会根据拦截器链调用被代理对象的方法（具体怎么执行后面说）
			3.如果成功创建代理对象，则直接返回代理对象；如果没有Advisor匹配当前bean，则说明当前bean无需代理对象，直接创建bean实例

		具体这个拦截器链是怎么工作的呢，以JDK动态代理为例，JDK生成的代理对象proxy在执行被代理对象的方法时，例如proxy.explode
		实际上只有一行，就是h.invoke，h是InvocationHandler，InvocationHandler的实现类是JdkDynamicAopProxy
		所以proxy.explode实际执行JdkDynamicAopProxy#invoke方法，在这里面就运行了拦截器链，具体请转至JdkDynamicAopProxy#invoke

		===========================================================================================================
														BUG FIX
		===========================================================================================================
		使用上面的方式无法完成代理对象的属性赋值，作者修改了上述代码
		上面说在createBean函数中，如果需要生成代理对象则直接生成代理对象，然后将doCreateBean短路，不生成被代理对象的bean实例
		但是这样无法对代理对象进行属性赋值，例如被代理对象有一个name属性，赋值为earth，但是代理对象的name属性是null

		所以现在无论怎样都会去执行doCreateBean方法，整个createBean方法简化为：
		-----------------------------------------------------------------------------------------------------------
			protected Object createBean(String beanName, BeanDefinition beanDefinition) throws BeansException {
				return doCreateBean(beanName, beanDefinition);
			}
		-----------------------------------------------------------------------------------------------------------

		在doCreateBean中，首先完成被代理对象的实例化以及属性赋值操作，之后调用：
		-----------------------------------------------------------------------------------------------------------
			// 执行bean的初始化方法和BeanPostProcessor的前置和后置处理方法
			bean = initializeBean(beanName, bean, beanDefinition);
		-----------------------------------------------------------------------------------------------------------

		主要关注这里面执行BeanPostProcessor的后置处理方法：
		-----------------------------------------------------------------------------------------------------------
			//执行BeanPostProcessor的后置处理
			wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
		-----------------------------------------------------------------------------------------------------------

		在之前讲过，在执行BeanPostProcessor后置处理时，会挨个遍历BeanPostProcessor，执行所有的postProcessAfterInitialization方法
		那么DefaultAdvisorAutoProxyCreator本身就是一个BeanPostProcessor，在他的postProcessAfterInitialization方法中是这么写的：
		-----------------------------------------------------------------------------------------------------------
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				if (!earlyProxyReferences.contains(beanName)) {
					return wrapIfNecessary(bean, beanName);
				}
				return bean;
			}
		-----------------------------------------------------------------------------------------------------------

		这个earlyProxyReferences后面再讲，简单来说就是如果在earlyProxyReferences中没有这个bean，就会去执行wrapIfNecessary方法
		如果earlyProxyReferences中有这个bean，认为代理对象已经被创建了，就直接返回
		那么在wrapIfNecessary里面就回去判断有没有Advisor匹配当前的bean，如果匹配，就把Advisor.MethodInterceptor加入到bean的拦截器链中
		如果最后拦截器链为空，则说明当前对象不需要创建代理对象，返回原先的bean；
		如果拦截器链非空，则根据传入的bean及其拦截器链，创建代理对象
			（注意，此时这个被代理bean是已经经过实例化和属性赋值的，所以用它来创建代理对象，代理对象里面的属性也是有值的）

		那么经过BeanPostProcessor后置处理，最后返回给doCreateBean的这个wrappedBean其实就变成了代理对象

		之后会把这个代理对象通过 addSingleton(beanName, exposedObject); 放到一级缓存中
		那么实际存在一级缓存中的bean还是代理对象proxy而不是被代理对象，被代理对象的实例确实被创建出来了，但是只是为了生成proxy，没有被记录下来
		这样生成的proxy是有属性值的

		那么现在其实变简单了，DefaultAdvisorAutoProxyCreator本身作为一个BeanPostProcessor
		它的执行位置和其他的BeanPostProcessor完全一致，不再把它单拎出来了
	 */
	@Test
	public void testAutoProxy() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:auto-proxy.xml");

		//获取代理对象
		WorldService worldService = applicationContext.getBean("worldService", WorldService.class);
		worldService.explode();
	}

	@Test
	public void testPopulateProxyBeanWithPropertyValues() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:populate-proxy-bean-with-property-values.xml");

		//获取代理对象
		WorldService worldService = applicationContext.getBean("worldService", WorldService.class);
		worldService.explode();
		assertThat(worldService.getName()).isEqualTo("earth");
	}
}
