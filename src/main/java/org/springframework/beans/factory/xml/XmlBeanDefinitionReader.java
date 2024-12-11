package org.springframework.beans.factory.xml;


import cn.hutool.core.util.StrUtil;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 读取配置在xml文件中的bean定义信息
 *
 * @author derekyi
 * @date 2020/11/26
 */
public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {

	public static final String BEAN_ELEMENT = "bean";
	public static final String PROPERTY_ELEMENT = "property";
	public static final String ID_ATTRIBUTE = "id";
	public static final String NAME_ATTRIBUTE = "name";
	public static final String CLASS_ATTRIBUTE = "class";
	public static final String VALUE_ATTRIBUTE = "value";
	public static final String REF_ATTRIBUTE = "ref";
	public static final String INIT_METHOD_ATTRIBUTE = "init-method";
	public static final String DESTROY_METHOD_ATTRIBUTE = "destroy-method";
	public static final String SCOPE_ATTRIBUTE = "scope";
	public static final String LAZYINIT_ATTRIBUTE = "lazyInit";
	public static final String BASE_PACKAGE_ATTRIBUTE = "base-package";
	public static final String COMPONENT_SCAN_ELEMENT = "component-scan";

	public XmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
		super(registry);
	}

	public XmlBeanDefinitionReader(BeanDefinitionRegistry registry, ResourceLoader resourceLoader) {
		super(registry, resourceLoader);
	}

	@Override
	public void loadBeanDefinitions(String location) throws BeansException {
		ResourceLoader resourceLoader = getResourceLoader();
		Resource resource = resourceLoader.getResource(location);
		// 方法重载，最后还是调用了 loadBeanDefinitions(Resource resource)
		loadBeanDefinitions(resource);
	}

	@Override
	public void loadBeanDefinitions(Resource resource) throws BeansException {
		/*
			最后都要调用此重载方法加载 BeanDefinition
		 */
		try {
			InputStream inputStream = resource.getInputStream();
			try {
				doLoadBeanDefinitions(inputStream);
			} finally {
				inputStream.close();
			}
		} catch (IOException | DocumentException ex) {
			throw new BeansException("IOException parsing XML document from " + resource, ex);
		}
	}

	protected void doLoadBeanDefinitions(InputStream inputStream) throws DocumentException {
		/*
			实际从 InputStream 中获取信息，创建 BeanDefinition 的函数
			SAXReader 是读取 xml 文件的工具类

			生成一个 BeanDefinition 示例：
			PropertyValues propertyValuesForCar = new PropertyValues();
			propertyValuesForCar.addPropertyValue(new PropertyValue("brand", "porsche"));
			BeanDefinition carBeanDefinition = new BeanDefinition(Car.class, propertyValuesForCar);
			beanFactory.registerBeanDefinition("car", carBeanDefinition);
			一个 BeanDefinition 需要三个参数：beanId、Class、propertyValues
		 */
		SAXReader reader = new SAXReader();
		Document document = reader.read(inputStream);

		Element root = document.getRootElement();

		//解析context:component-scan标签并扫描指定包中的类，提取类信息，组装成BeanDefinition
		// 在 spring.xml 文件中没有 component-scan 标签，直接写的 bean，所以这里暂时不会运行
		// 在package-scan.xml里面用到了
		Element componentScan = root.element(COMPONENT_SCAN_ELEMENT);
		if (componentScan != null) {
			String scanPath = componentScan.attributeValue(BASE_PACKAGE_ATTRIBUTE);
			if (StrUtil.isEmpty(scanPath)) {
				throw new BeansException("The value of base-package attribute can not be empty or null");
			}
			// 扫描 component-scan 路径
			scanPackage(scanPath);
		}

		/*
			获取所有 bean 标签的内容，即以下内容：

			<bean id="person" class="org.springframework.test.bean.Person">
				<property name="name" value="derek"/>
				<property name="car" ref="car"/>
			</bean>

			<bean id="car" class="org.springframework.test.bean.Car">
				<property name="brand" value="porsche"/>
			</bean>

			<bean class="org.springframework.test.common.CustomBeanFactoryPostProcessor"/>

			<bean class="org.springframework.test.common.CustomerBeanPostProcessor"/>

			<bean id="helloService" class="org.springframework.test.service.HelloService"/>
		 */
		List<Element> beanList = root.elements(BEAN_ELEMENT);
		for (Element bean : beanList) {
			String beanId = bean.attributeValue(ID_ATTRIBUTE);	// beanId
			String beanName = bean.attributeValue(NAME_ATTRIBUTE);	// beanName
			String className = bean.attributeValue(CLASS_ATTRIBUTE);	// class
			String initMethodName = bean.attributeValue(INIT_METHOD_ATTRIBUTE);	// 初始化方法
			String destroyMethodName = bean.attributeValue(DESTROY_METHOD_ATTRIBUTE);	// 销毁方法
			String beanScope = bean.attributeValue(SCOPE_ATTRIBUTE);	// 是否单例
			String lazyInit = bean.attributeValue(LAZYINIT_ATTRIBUTE);	// 懒加载（即类加载时不直接创建实例）
			Class<?> clazz;
			try {
				// 通过反射获取类
				clazz = Class.forName(className);
			} catch (ClassNotFoundException e) {
				throw new BeansException("Cannot find class [" + className + "]");
			}
			//id优先于name，如果有 id，就以 id 为名称，没有 id 才以 name 为名称
			beanName = StrUtil.isNotEmpty(beanId) ? beanId : beanName;
			if (StrUtil.isEmpty(beanName)) {
				//如果id和name都为空，将类名的第一个字母转为小写后作为bean的名称
				beanName = StrUtil.lowerFirst(clazz.getSimpleName());
			}

			BeanDefinition beanDefinition = new BeanDefinition(clazz);
			beanDefinition.setInitMethodName(initMethodName);
			beanDefinition.setDestroyMethodName(destroyMethodName);
			beanDefinition.setLazyInit(Boolean.parseBoolean(lazyInit));
			if (StrUtil.isNotEmpty(beanScope)) {
				beanDefinition.setScope(beanScope);
			}

			/*
				获取 propertyValues
				<property name="name" value="derek"/>
				<property name="car" ref="car"/>
			 */
			List<Element> propertyList = bean.elements(PROPERTY_ELEMENT);
			for (Element property : propertyList) {
				String propertyNameAttribute = property.attributeValue(NAME_ATTRIBUTE);	// name
				String propertyValueAttribute = property.attributeValue(VALUE_ATTRIBUTE);
				String propertyRefAttribute = property.attributeValue(REF_ATTRIBUTE);	// 其实 value 和 ref 只会有一个

				if (StrUtil.isEmpty(propertyNameAttribute)) {
					throw new BeansException("The name attribute cannot be null or empty");
				}

				Object value = propertyValueAttribute;
				if (StrUtil.isNotEmpty(propertyRefAttribute)) {
					value = new BeanReference(propertyRefAttribute);
				}
				PropertyValue propertyValue = new PropertyValue(propertyNameAttribute, value);
				beanDefinition.getPropertyValues().addPropertyValue(propertyValue);
			}
			if (getRegistry().containsBeanDefinition(beanName)) {
				//beanName不能重名
				throw new BeansException("Duplicate beanName[" + beanName + "] is not allowed");
			}
			//注册BeanDefinition
			getRegistry().registerBeanDefinition(beanName, beanDefinition);
		}
	}

	/**
	 * 扫描注解Component的类，提取信息，组装成BeanDefinition
	 *
	 * @param scanPath
	 */
	private void scanPackage(String scanPath) {
		String[] basePackages = StrUtil.splitToArray(scanPath, ',');
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(getRegistry());
		scanner.doScan(basePackages);
	}
}
