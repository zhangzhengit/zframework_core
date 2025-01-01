package com.vo.html;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.common.collect.HashBasedTable;
import com.vo.cache.STU;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.core.Task;
import com.vo.core.ZContext;
import com.vo.core.ZSingleton;
import com.vo.exception.ResourceNotExistException;

import cn.hutool.core.io.FastByteArrayOutputStream;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.UUID;

/**
 * 从 硬盘或resources 目录加载文件，根据配置项来选择从哪里加载
 *
 * @author zhangzhen
 * @date 2023年6月24日
 *
 */
public class ResourcesLoader {

	private static ServerConfigurationProperties SERVER_CONFIGURATION= ZContext.getBean(ServerConfigurationProperties.class);

	public static final String STATIC_RESOURCES_PROPERTY_NAME = "resource.path-" + UUID.randomUUID();

	private final static HashBasedTable<ResourcesTypeEnum, String, Object> CACHE_TABLE = HashBasedTable.create();

	/**
	 * 加载资源为String , resourceName不用自己拼接前缀目录了，此方法内自动拼接
	 *
	 * @param resourceName
	 * @return
	 */
	public static String loadStaticResourceString(final String resourceName) {

		final String resourcePath = System.getProperty(STATIC_RESOURCES_PROPERTY_NAME);
		if (STU.isNullOrEmptyOrBlank(resourcePath)) {
			final ServerConfigurationProperties serverConfiguration = ZSingleton
					.getSingletonByClass(ServerConfigurationProperties.class);
			final String staticPrefix = serverConfiguration.getStaticPrefix();
			final String key = staticPrefix + resourceName;
			return loadString(key);
		}

		final String name = resourcePath + (resourceName.replace("/", File.separator));
		FileReader fileReader = null;
		try {
			fileReader = new FileReader(name);
		} catch (final FileNotFoundException e1) {
			throw new ResourceNotExistException("资源不存在,name = " + resourceName);
		}

		final BufferedReader bufferedReader = new BufferedReader(fileReader);
		try {

			final StringBuilder builder = new StringBuilder();
			while (true) {
				final String readLine = bufferedReader.readLine();
				if (readLine == null) {
					break;
				}
				builder.append(readLine);
				builder.append(Task.NEW_LINE);
			}

			bufferedReader.close();
			fileReader.close();

			return builder.toString();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * 加载静态资源为InputStream，适合较大的资源文件，边read边写入OutputStream
	 *
	 * @param resourceName 资源名称，如：1.jpg/index.html 等等
	 * @return
	 *
	 */
	public static InputStream loadStaticResourceAsInputStream(final String resourceName) {
		final String resourcePath = System.getProperty(STATIC_RESOURCES_PROPERTY_NAME);
		if (STU.isNullOrEmptyOrBlank(resourcePath)) {
			final ServerConfigurationProperties serverConfiguration = ZSingleton
					.getSingletonByClass(ServerConfigurationProperties.class);
			final String staticPrefix = serverConfiguration.getStaticPrefix();
			final String key = staticPrefix + resourceName;
			return checkInputStream(key);
		}

		final String fileName = resourcePath + (resourceName.replace("/", File.separator));
		try {
			return new FileInputStream(fileName);
		} catch (final FileNotFoundException e1) {
			throw new ResourceNotExistException("资源不存在,name = " + resourceName);
		}
	}

	/**
	 * 加载静态资源为byte[]，一次性读取出来
	 *
	 * @param resourceName 资源名称，如：1.jpg/index.html 等等
	 * @return
	 *
	 */
	public static byte[] loadStaticResourceAsByteArray(final String resourceName) {

		final String resourcePath = System.getProperty(STATIC_RESOURCES_PROPERTY_NAME);
		if (STU.isNullOrEmptyOrBlank(resourcePath)) {
			final ServerConfigurationProperties serverConfiguration = ZSingleton.getSingletonByClass(ServerConfigurationProperties.class);
			final String staticPrefix = serverConfiguration.getStaticPrefix();
			final String key = staticPrefix + resourceName;

			final byte[] ba = loadByteArray0(key);
			return ba;
		}

		//			final byte[] ba1 = loadByteArray(resourcePath + File.separator + (resourceName.replace("/", "")));
		//			final String fileName = resourcePath + File.separator + (resourceName.replace("/", ""));
		final String fileName = resourcePath + (resourceName.replace("/", File.separator));
		try {
			final FileInputStream fileInputStream = new FileInputStream(new File(fileName));

			final FastByteArrayOutputStream read = IoUtil.read(fileInputStream);

			final byte[] byteArray = read.toByteArray();

			fileInputStream.close();

			return byteArray;
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	private static byte[] loadByteArray0(final String resourceName) {
		if (Boolean.FALSE.equals(SERVER_CONFIGURATION.getStaticResourceCacheEnable())) {
			return readByteArray0(checkInputStream(resourceName));
		}

		final Object v = CACHE_TABLE.get(ResourcesTypeEnum.BINARY, resourceName);
		if (v != null) {
			return (byte[]) v;
		}

		synchronized (("loadByteArray0" + resourceName).intern()) {

			final Object vN = CACHE_TABLE.get(ResourcesTypeEnum.BINARY, resourceName);
			if (vN != null) {
				return (byte[]) vN;
			}

			final InputStream in = checkInputStream(resourceName);
			final byte[] ba2 = readByteArray0(in);

			CACHE_TABLE.put(ResourcesTypeEnum.BINARY, resourceName, ba2);
			return ba2;
		}
	}

	private static String loadString(final String name) {

		if (Boolean.FALSE.equals(SERVER_CONFIGURATION.getStaticResourceCacheEnable())) {
			return loadSring0(name);
		}

		final Object v = CACHE_TABLE.get(ResourcesTypeEnum.STRING, name);
		if (v != null) {
			return (String) v;
		}

		synchronized (name) {
			final String v2 = loadSring0(name);
			CACHE_TABLE.put(ResourcesTypeEnum.STRING, name, v2);
			return v2;
		}
	}

	private static String loadSring0(final String name) {
		final InputStream inputStream = checkInputStream(name);
		final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
		final BufferedReader reader = new BufferedReader(inputStreamReader);

		final StringBuilder builder = new StringBuilder();
		while (true) {
			try {
				final String readLine = reader.readLine();
				if (readLine == null) {
					break;
				}
				builder.append(readLine);
				builder.append(Task.NEW_LINE);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		try {
			reader.close();
			inputStreamReader.close();
			inputStream.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return builder.toString();
	}

	private static byte[] readByteArray0(final InputStream inputStream) {
		final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
		final byte[] ba = new byte[1000 * 10];
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		while (true) {
			try {
				final int read = bufferedInputStream.read(ba);
				if (read <= -1) {
					break;
				}

				byteArrayOutputStream.write(ba, 0, read);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		try {
			// 空方法
			byteArrayOutputStream.close();
			bufferedInputStream.close();
			inputStream.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return byteArrayOutputStream.toByteArray();
	}

	private static InputStream checkInputStream(final String name) {
		final InputStream inputStream = ResourcesLoader.class.getResourceAsStream(name);
		if (inputStream == null) {
			throw new ResourceNotExistException("资源不存在,name = " + name);
		}
		return inputStream;
	}

	public enum ResourcesTypeEnum {

		BINARY, STRING;
	}

}
