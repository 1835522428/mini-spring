package org.springframework.beans.factory.support;

import cn.hutool.core.bean.BeanException;
import org.springframework.beans.BeansException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * 读取bean定义信息即BeanDefinition的接口
 *
 * @author derekyi
 * @date 2020/11/26
 */
public interface BeanDefinitionReader {

	BeanDefinitionRegistry getRegistry();

	ResourceLoader getResourceLoader();

	/*
		方法重载，loadBeanDefinitions 同名方法，参数不同
		如果直接提供了 Resource，就直接获取这一个 Resource.InputStream 生成 BeanDefinition
		如果提供了一个 location，就使用 ResourceLoader 把这一个 location 位置的资源生成 BeanDefinition
		如果提供了一堆 locations，就使用 ResourceLoader 挨个把 location 生成 BeanDefinition（for each 循环）
		使用 ResourceLoader 读取资源的流程见测试类：ResourceAndResourceLoaderTest

		其实无论使用哪种方法，最后调用的都是 loadBeanDefinitions(Resource resource)
		loadBeanDefinitions(String location) 使用 ResourceLoader 获取到 Resource，再 loadBeanDefinitions(resource)
		loadBeanDefinitions(String[] locations) 挨个获取到各个 location 的 Resource，再调用 loadBeanDefinitions(resource)
	 */
	void loadBeanDefinitions(Resource resource) throws BeansException;

	void loadBeanDefinitions(String location) throws BeansException;

	void loadBeanDefinitions(String[] locations) throws BeansException;
}
