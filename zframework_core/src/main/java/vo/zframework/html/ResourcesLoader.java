package vo.zframework.html;

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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.UUID;

import vo.zframework.cache.ZRC;
import vo.zframework.common.STU;
import vo.zframework.configuration.properties.ServerConfigurationProperties;
import vo.zframework.core.ZContext;
import vo.zframework.enums.HttpStatusEnum;
import vo.zframework.exception.ResourceNotExistException;

/**
 * 从 硬盘或resources 目录加载文件，根据配置项来选择从哪里加载
 *
 * @author zhangzhen
 * @date 2023年6月24日
 *
 */
public class ResourcesLoader {

	private static ServerConfigurationProperties SERVER_CONFIGURATION= ZContext.getBean(ServerConfigurationProperties.class);

	private static final boolean STATIC_RESOURCE_CACHE_ENABLE = SERVER_CONFIGURATION.getStaticResourceCacheEnable();

	public static final String STATIC_RESOURCES_PROPERTY_NAME = "resource.path-" + UUID.randomUUID();

	private final static ZRC BYTE_CACHE = new ZRC(100, 1);

	private final static ZRC STRING_CACHE = new ZRC(100, 1);

	/**
	 * 加载资源为String , resourceName不用自己拼接前缀目录了，此方法内自动拼接
	 *
	 * @param resourceName
	 * @return
	 */
	public static String loadStaticResourceString(final String resourceName) {

		final String resourcePath = System.getProperty(STATIC_RESOURCES_PROPERTY_NAME);
		if (STU.isNullOrEmptyOrBlank(resourcePath)) {

			final ServerConfigurationProperties serverConfiguration = ZContext.getBean(ServerConfigurationProperties.class);
			final String staticPrefix = serverConfiguration.getStaticPrefix();
			final String key = staticPrefix + resourceName;
			return loadStringByCache(key, resourceName);
		}

		final String name = resourcePath + (resourceName.replace("/", File.separator));
		FileReader fileReader = null;
		try {
			fileReader = new FileReader(name);
		} catch (final FileNotFoundException e1) {
			throw new ResourceNotExistException(resourceName, HttpStatusEnum.HTTP_404.getStatus());
		}

		try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {

			final StringBuilder builder = new StringBuilder();
			while (true) {
				final String readLine = bufferedReader.readLine();
				if (readLine == null) {
					break;
				}
				builder.append(readLine);
				builder.append(STU.CRLF);
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
	 * 注意：使用后请关闭此 InputStream ，
	 * 		并且使用 BufferedInputStream 来包装此类来读取，否则可能出现读取不完整的情况
	 *
	 * @param resourceName 资源名称，如：1.jpg/index.html 等等
	 * @return
	 *
	 */
	public static FIS loadStaticResourceAsInputStream(final String resourceName) {

		final String resourcePath = System.getProperty(STATIC_RESOURCES_PROPERTY_NAME);
		if (STU.isNullOrEmptyOrBlank(resourcePath)) {
			final ServerConfigurationProperties serverConfiguration = ZContext
					.getBean(ServerConfigurationProperties.class);
			final String staticPrefix = serverConfiguration.getStaticPrefix();
			final String key = staticPrefix + resourceName;
			return checkInputStream(key, resourceName);
		}

		final String fileName = resourcePath + (resourceName.replace("/", File.separator));

		try {
			final File file = new File(fileName);
			final FileInputStream fileInputStream = new FileInputStream(file);
			final FIS fis = new FIS(fileInputStream, file);
			return fis;
		} catch (final FileNotFoundException e1) {
			throw new ResourceNotExistException(resourceName, HttpStatusEnum.HTTP_404.getStatus());
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
			final ServerConfigurationProperties serverConfiguration = ZContext.getBean(ServerConfigurationProperties.class);
			final String staticPrefix = serverConfiguration.getStaticPrefix();
			final String key = staticPrefix + resourceName;

			return loadByteArrayByCache(key);
		}

		return BYTE_CACHE.computeIfAbsent(resourceName, () -> extracted(resourceName, resourcePath));
	}

	private static byte[] extracted(final String resourceName, final String resourcePath) {
		final String fileName = resourcePath + (resourceName.replace("/", File.separator));

		final FileInputStream fileInputStream;
		try {
			fileInputStream = new FileInputStream(new File(fileName));
		} catch (final FileNotFoundException e1) {
			throw new ResourceNotExistException(resourceName, HttpStatusEnum.HTTP_404.getStatus());
		}

		final byte[] byteArray = readByteArrayFromInputStream(fileInputStream);
		try {
			fileInputStream.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return byteArray;
	}

	private static byte[] loadByteArrayByCache(final String resourceName) {
		if (!STATIC_RESOURCE_CACHE_ENABLE) {
			return readByteArrayFromInputStream(checkInputStream(resourceName, resourceName).getInputStream());
		}

		final byte[] v = BYTE_CACHE.computeIfAbsent(resourceName,
					() -> readByteArrayFromInputStream(checkInputStream(resourceName, resourceName).getInputStream()));
		return v;
	}

	private static String loadStringByCache(final String name, final String resourceName) {
		if (!STATIC_RESOURCE_CACHE_ENABLE) {
			return loadSring0(name, resourceName);
		}

		final Object v = STRING_CACHE.computeIfAbsent(name, ()-> loadSring0(name, resourceName));
		return (String) v;
	}

	private static String loadSring0(final String name, final String resourceName) {
		try (final InputStream inputStream = checkInputStream(name, resourceName).getInputStream();
			 final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
		     final BufferedReader reader = new BufferedReader(inputStreamReader);
				) {

			final StringBuilder builder = new StringBuilder();
			while (true) {
				try {
					final String readLine = reader.readLine();
					if (readLine == null) {
						break;
					}
					builder.append(readLine);
					builder.append(STU.CRLF);
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
			return builder.toString();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	private static byte[] readByteArrayFromInputStream(final InputStream inputStream) {
		try (final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
			final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();) {
			final byte[] buffer = new byte[1024 * 8];

			while (true) {
				try {
					final int read = bufferedInputStream.read(buffer);
					if (read <= -1) {
						break;
					}

					byteArrayOutputStream.write(buffer, 0, read);
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}

			return byteArrayOutputStream.toByteArray();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return null;
	}


	private static FIS checkInputStream(final String name, final String resourceName) {
		final InputStream inputStream = ResourcesLoader.class.getResourceAsStream(name);

		final File staticFile = getStaticFile(name.substring(1));

		if (inputStream == null) {
			// FIXME 2025年12月8日 17:51:22 zhangzhen : 这里提示详细一点，具体时候那个资源
			throw new ResourceNotExistException(resourceName, HttpStatusEnum.HTTP_404.getStatus());
		}
		return new FIS(inputStream, staticFile);
	}


	public static File getStaticFile(final String relativePath) {
		// relativePath 例如 "images/logo.png"
		final URL url = ResourcesLoader.class.getClassLoader().getResource(relativePath);
		if (url == null) {
			throw new ResourceNotExistException(relativePath);
		}

		// 检查协议
		if ("file".equals(url.getProtocol())) {
			// 开发环境：直接转为 File
			try {
				return Paths.get(url.toURI()).toFile();
			} catch (final URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}

		if ("jar".equals(url.getProtocol())) {
			// 生产环境（JAR 包内）：无法直接获取 File
			// 只能通过 InputStream 读取内容，无法获得 File 对象
//			throw new UnsupportedOperationException("无法从 JAR 中获取 File 对象，请使用 InputStream");
		}

		return null;
	}

	public enum ResourcesTypeEnum {

		BINARY, STRING;
	}

}
