package org.springframework.core.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * classpath下的资源
 *
 * @author derekyi
 * @date 2020/11/25
 */
public class ClassPathResource implements Resource {

	private final String path;

	public ClassPathResource(String path) {
		this.path = path;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		/*
			这里调用了类加载器的 getResourceAsStream 方法，从类路径中获取资源文件的输入流
			传递的参数 name 是相对于类路径（ClassPath）的路径
			所以 getResourceAsStream 方法只能用于加载类路径下的资源，例如：classpath:hello.txt，不能加载 URL 或者文件系统下的资源
			URL 资源要用 UrlResource 加载，文件系统资源要用 FileSystemResource 加载
		 */
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(this.path);
		if (is == null) {
			throw new FileNotFoundException(this.path + " cannot be opened because it does not exist");
		}
		return is;
	}
}
