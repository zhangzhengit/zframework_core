package com.vo.zframework.common;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 这个类是问豆包要的代码
 *
 * 扫描指定包名下的所有 Class 类
 *
 * @author zhangzhen
 * @date 2025年12月5日 16:28:48
 */
public class PackageScanner {

	/**
	 * 扫描指定包名下的所有 Class
	 *
	 * @param packageName 包名（如：com.vo）
	 * @return 包下所有 Class 的集合
	 * @throws IOException            IO异常
	 * @throws ClassNotFoundException 类加载异常
	 */
	public static Set<Class<?>> scanPackage(final String packageName) throws IOException, ClassNotFoundException {
		final Set<Class<?>> classSet = new LinkedHashSet<>();
		// 替换包名中的点为文件路径分隔符
		final String packagePath = packageName.replace('.', '/');
		// 获取当前线程的类加载器（核心：适配不同运行环境）
		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		// 1. 获取包名对应的所有资源URL（支持目录/JAR包两种场景）
		final Enumeration<URL> resources = classLoader.getResources(packagePath);
		while (resources.hasMoreElements()) {
			final URL resource = resources.nextElement();
			// 解析资源协议（file: 目录形式；jar: JAR包形式）
			final String protocol = resource.getProtocol();

			if ("file".equals(protocol)) {
				// 场景1：本地目录形式（如IDE中运行）
				final String filePath = URLDecoder.decode(resource.getFile());
				scanDirectoryClasses(packageName, filePath, classSet);
			} else if ("jar".equals(protocol)) {
				// 场景2：JAR包形式（如打包后运行）
				final JarURLConnection jarURLConnection = (JarURLConnection) resource.openConnection();
				final JarFile jarFile = jarURLConnection.getJarFile();
				scanJarClasses(packageName, jarFile, classSet);
			}
		}
		return classSet;
	}

	/**
	 * 扫描目录形式的包，加载所有 Class
	 *
	 * @param packageName 包名
	 * @param filePath    包对应的目录路径
	 * @param classSet    存储结果的集合
	 * @throws ClassNotFoundException 类加载异常
	 */
	private static void scanDirectoryClasses(final String packageName, final String filePath,
			final Set<Class<?>> classSet) {
		final File dir = new File(filePath);
		// 过滤非目录/不可读的情况
		if (!dir.exists() || !dir.isDirectory()) {
			return;
		}

		// 遍历目录下的所有文件（包括子目录）
		final File[] files = dir
				.listFiles(file -> (file.isDirectory() || (file.isFile() && file.getName().endsWith(".class"))));

		if (files == null) {
			return;
		}

		for (final File file : files) {
			final String fileName = file.getName();
			if (file.isDirectory()) {
				// 递归扫描子目录（拼接子包名）
				final String subPackageName = packageName + "." + fileName;
				scanDirectoryClasses(subPackageName, file.getAbsolutePath(), classSet);
			} else {
				// 加载 .class 文件（去掉后缀，转为类名）
				final String className = fileName.substring(0, fileName.length() - 6);
				final String fullClassName = packageName + "." + className;
				// 加载类（使用当前类加载器，避免类加载冲突）
				final Class<?> clazz = load(fullClassName);
				classSet.add(clazz);
			}
		}
	}

	private static Class<?> load(final String fullClassName) {
		try {
			return Class.forName(fullClassName, false, Thread.currentThread().getContextClassLoader());
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 扫描 JAR 包中的包，加载所有 Class
	 *
	 * @param packageName 包名
	 * @param jarFile     JAR 文件对象
	 * @param classSet    存储结果的集合
	 * @throws ClassNotFoundException 类加载异常
	 */
	private static void scanJarClasses(final String packageName, final JarFile jarFile, final Set<Class<?>> classSet)
			throws ClassNotFoundException {
		final String packagePath = packageName.replace('.', '/');
		final Enumeration<JarEntry> entries = jarFile.entries();

		while (entries.hasMoreElements()) {
			final JarEntry jarEntry = entries.nextElement();
			final String entryName = jarEntry.getName();

			// 过滤：只处理当前包下的 .class 文件，且排除目录
			if (entryName.startsWith(packagePath) && entryName.endsWith(".class") && !jarEntry.isDirectory()) {
				// 把 JAR 路径转为类名（如：com/vo/User.class → com.vo.User）
				final String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
				final Class<?> clazz = Class.forName(className,false,Thread.currentThread().getContextClassLoader());
				classSet.add(clazz);
			}
		}
	}

}