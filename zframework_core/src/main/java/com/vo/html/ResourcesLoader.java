package com.vo.html;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.HashBasedTable;
import com.vo.cache.STU;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.core.ContentTypeEnum;
import com.vo.core.Task;
import com.vo.core.ZContext;
import com.vo.core.ZRequest;
import com.vo.core.ZResponse;
import com.vo.core.ZSingleton;
import com.vo.exception.ResourceNotExistException;

import cn.hutool.core.io.FastByteArrayOutputStream;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.UUID;

/**
 * 从 resources 目录加载文件
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
			final ServerConfigurationProperties serverConfiguration = ZSingleton.getSingletonByClass(ServerConfigurationProperties.class);
			final String staticPrefix = serverConfiguration.getStaticPrefix();
			final String key = staticPrefix + resourceName;

			final String string = loadString(key);
			return string;

		}
		try {
			final String name = resourcePath + (resourceName.replace("/", File.separator));
			final FileReader fileReader = new FileReader(name);
			final BufferedReader bufferedReader = new BufferedReader(fileReader);

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
	 * 把静态资源写入输出流，不放入缓存.
	 *
	 * @param resourceName
	 * @param cte
	 * @param response
	 * @param outputStream
	 * @return 返回写入的字节数
	 */
	public static long writeResourceToOutputStreamThenClose(final String resourceName, final ContentTypeEnum cte, final ZResponse response) {

		final ServerConfigurationProperties serverConfiguration = ZSingleton.getSingletonByClass(ServerConfigurationProperties.class);
		final String staticPrefix = serverConfiguration.getStaticPrefix();
		final String key = staticPrefix + resourceName;

		final InputStream inputStream = checkInputStream(key);

		final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

		if (response.getSocketChannel() != null) {
			return writeSocketChannel(cte, response, bufferedInputStream);
		}

		throw new IllegalArgumentException(ZResponse.class.getSimpleName() + " outputStream 和 socketChannel 不能同时为空");

	}

	private static long writeSocketChannel(final ContentTypeEnum cte, final ZResponse response,
			final BufferedInputStream bufferedInputStream) {
		final AtomicLong write1 = new AtomicLong(0L);
		final byte[] ba = new byte[1000 * 10];
		final AtomicLong write = new AtomicLong(0);
		final List<Byte> list = new ArrayList<>();
		while (true) {
			try {
				final int read = bufferedInputStream.read(ba);
				if (read <= -1) {
					break;
				}

				write.set(write.get() + read);
				for (int i = 0; i < read; i++) {
					list.add(ba[i]);
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		final byte[] baR = new byte[list.size()];
		for (int i = 0; i < list.size(); i++) {
			baR[i] = list.get(i);
		}

		response.contentType(cte.getType())
		.header(ZRequest.CONTENT_LENGTH,String.valueOf(baR.length))
		.body(baR)
		.write();

		write1.set(baR.length);

		return write1.get();
	}

	private static AtomicLong writeOutputStream(final ContentTypeEnum cte, final OutputStream outputStream,
			final InputStream inputStream, final BufferedInputStream bufferedInputStream) {
		try {
			outputStream.write(Task.HTTP_200.getBytes());
			outputStream.write(Task.NEW_LINE.getBytes());
			outputStream.write(cte.getValue().getBytes());
			outputStream.write(Task.NEW_LINE.getBytes());
			// FIXME 2023年7月3日 下午7:38:24 zhanghen: TODO 加入content-length
			outputStream.write(Task.NEW_LINE.getBytes());
		} catch (final IOException e1) {
			e1.printStackTrace();
		}

		final AtomicLong write = writeToOutputStream(bufferedInputStream, outputStream);

		try {
			//			outputStream.write(Task.NEW_LINE.getBytes());
			outputStream.flush();
			outputStream.close();

			bufferedInputStream.close();
			inputStream.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return write;
	}

	private static AtomicLong writeToOutputStream(final BufferedInputStream bufferedInputStream,
			final OutputStream outputStream) {
		final byte[] ba = new byte[1000 * 10];
		final AtomicLong write = new AtomicLong(0);
		while (true) {
			try {
				final int read = bufferedInputStream.read(ba);
				if (read <= -1) {
					break;
				}

				write.set(write.get() + read);
				outputStream.write(ba, 0, read);
				outputStream.flush();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		return write;
	}

	/**
	 * 加载静态资源，resourceName 不用自己拼接前缀目录了，此方法内自动拼接
	 *
	 * @param resourceName
	 * @return
	 *
	 */
	public static byte[] loadStaticResourceByteArray(final String resourceName) {

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

		final byte[] byteArray = byteArrayOutputStream.toByteArray();
		return byteArray;
	}


	private static InputStream checkInputStream(final String name) {
		//		final URL resource = ResourcesLoader.class.getResource(name);
		//		System.out.println("resource = " + resource);
		final InputStream inputStream = ResourcesLoader.class.getResourceAsStream(name);
		if (inputStream == null) {
			throw new ResourceNotExistException("资源不存在,name = " + name);
			//			throw new IllegalArgumentException("资源不存在,name = " + name);
		}
		return inputStream;
	}

	public enum ResourcesTypeEnum{

		BINARY,STRING;
	}

}
