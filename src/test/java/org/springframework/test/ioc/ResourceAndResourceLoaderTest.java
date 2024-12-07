package org.springframework.test.ioc;

import cn.hutool.core.io.IoUtil;
import org.junit.Test;
import org.springframework.core.io.*;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author derekyi
 * @date 2020/11/25
 */
public class ResourceAndResourceLoaderTest {

	@Test
	public void testResourceLoader() throws Exception {
		/*
			DefaultResourceLoader 用于根据文件路径加载文件，返回一个 Resource 对象
			（可以认为一个 Resource 就代表了一个资源，Resource 对象里面保存了资源的 InputStream）
			传入 DefaultResourceLoader.getResource() 方法里面的地址可以是：ClassPath路径、URL、文件系统路径
			根据传入地址种类的不同，需要有不同获取 InputStream 的方法实现
			如果传入的是 ClassPath路径，例如：classpath:hello.txt，就需要使用类加载器加载资源
			如果传入的是 URL，就需要通过 net 包的 getInputStream() 方法加载资源
			如果传入的是文件系统资源，例如：src/test/resources/hello.txt，就需要使用 NIO 的 Files.newInputStream() 加载资源
			所以 Resource 有三个实现类：ClassPathResource、UrlResource、FileSystemResource
			三种实现类实现了各自的资源加载方式
		 */
		DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

		//加载classpath下的资源
		Resource resource = resourceLoader.getResource("classpath:hello.txt");
		InputStream inputStream = resource.getInputStream();
		String content = IoUtil.readUtf8(inputStream);
		System.out.println(content);
		assertThat(content).isEqualTo("hello world");

		//加载文件系统资源
		resource = resourceLoader.getResource("src/test/resources/hello.txt");
		assertThat(resource instanceof FileSystemResource).isTrue();
		inputStream = resource.getInputStream();
		content = IoUtil.readUtf8(inputStream);
		System.out.println(content);
		assertThat(content).isEqualTo("hello world");

		//加载url资源
		resource = resourceLoader.getResource("https://github.com/DerekYRC/mini-spring/blob/main/README.md");
		assertThat(resource instanceof UrlResource).isTrue();
		inputStream = resource.getInputStream();
		content = IoUtil.readUtf8(inputStream);
		System.out.println(content);
	}
}
