package com.vo.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * 读取配置文件
 *
 * @author zhangzhen
 * @date 2025年1月1日 下午10:11:53
 *
 */
public class ZProperties {

	private static final Charset UTF8 = StandardCharsets.UTF_8;

	public static final String PROPERTIES_1 = "config/zframework.properties";

	public static final String PROPERTIES_2 = "zframework.properties";
	public static final String PROPERTIES_NAME = PROPERTIES_2;
	public static final String PROPERTIES_3 = "src/main/resources/zframework.properties";
	public static final String PROPERTIES_4 = "src/main/resources/config/zframework.properties";

	private static final String[] EMPTY_STRING_ARRAY = {};

	public static boolean readBoolean(final String key) {
		final String property = properties.getProperty(key);
		return Boolean.parseBoolean(property);
	}

	public static boolean containsKey(final String key) {
		return properties.containsKey(key);
	}

	public static Byte getByte(final String key) {
		final String v = properties.getProperty(key);
		if (v == null) {
			return null;
		}

		return Byte.parseByte(v);
	}

	public static Short getShort(final String key) {
		final String v = properties.getProperty(key);
		if (v == null) {
			return null;
		}

		return Short.parseShort(v);
	}

	public static Integer getInteger(final String key, final Integer defaultValue) {
		final String v = properties.getProperty(key);
		if (v == null) {
			return defaultValue;
		}

		return Integer.parseInt(v);
	}

	public static Integer getInteger(final String key) {
		final String v = properties.getProperty(key);
		if (v == null) {
			return null;
		}

		return Integer.parseInt(v);
	}

	public static Long getLong(final String key) {
		final String v = properties.getProperty(key);
		if (v == null) {
			return null;
		}

		return Long.parseLong(v);
	}

	public static BigInteger getBigInteger(final String key) {
		final String v = properties.getProperty(key);
		if (v == null) {
			return null;
		}

		return new BigInteger(v);
	}

	public static BigDecimal getBigDecimal(final String key) {
		final String v = properties.getProperty(key);
		if (v == null) {
			return null;
		}

		return new BigDecimal(v);
	}

	public static Float getFloat(final String key) {
		final String v = properties.getProperty(key);
		if (v == null) {
			return null;
		}

		return Float.parseFloat(v);
	}
	public static Double getDouble(final String key) {
		final String v = properties.getProperty(key);
		if (v == null) {
			return null;
		}

		return Double.parseDouble(v);
	}

	public static Integer readInteger(final String key) {
		final String v = properties.getProperty(key);
		if (v == null) {
			return null;
		}
		final int i = Integer.parseInt(v);
		return i;
	}

	public static Boolean getBoolean(final String key) {
		final String v = properties.getProperty(key);
		if (v == null) {
			return null;
		}

		return Boolean.parseBoolean(v);
	}

	public static Long readLong(final String key) {
		final String v = properties.getProperty(key);
		if (v == null) {
			return null;
		}

		return Long.parseLong(v);
	}

	public static Iterator<String> getKeys(final String prefix) {
		final List<String> list = new ArrayList<>();
		final Enumeration<Object> ks = properties.keys();
		while (ks.hasMoreElements()) {
			final Object e = ks.nextElement();
			final String s = String.valueOf(e);
			if (s.startsWith(prefix)) {
				list.add(s);
			}
		}

		return list.iterator();
	}

	public static void addProperty() {

	}

	public static String getString(final String key) {
		return properties.getProperty(key);
	}

	public static String[] getStringArray(final String key) {
		final Object v = properties.get(key);
		if (v == null) {
			return EMPTY_STRING_ARRAY;
		}

		final String s1 = String.valueOf(v);
		final String[] a = s1.split(",");
		return a;
	}

	public static String readString(final String key) {
		return properties.getProperty(key);
	}

	private static Properties properties;

	public static Properties getInstance() {
		return properties;
	}

	static {

		Properties p1 = loadDirConfig(File.separator + ZProperties.PROPERTIES_1);
		if (p1 == null) {
			p1 = loadDirConfig(File.separator + ZProperties.PROPERTIES_2);
			if (p1 == null) {
				p1 = loadPResources("/" + ZProperties.PROPERTIES_1);
				if (p1 == null) {
					p1 = loadPResources("/" + ZProperties.PROPERTIES_2);
				}
			}
		}

		if (p1 == null) {
			System.out.println("ERROR: 启动失败," + ZProperties.PROPERTIES_1 + "配置文件不存在,请编写此配置文件");
			System.exit(0);
		}

		properties = p1;

	}

	private static Properties loadPResources(final String path) {
		final InputStream inputStream = ZProperties.class.getResourceAsStream(path);
		if (inputStream == null) {
			return null;
		}

		final Properties p2 = new Properties();
		InputStreamReader reader = null;
		try {
			reader = new InputStreamReader(inputStream, UTF8);
			p2.load(reader);
		} catch (final IOException e1) {
			e1.printStackTrace();
		} finally {
			try {
				inputStream.close();
				if (reader != null) {
					reader.close();
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		return p2;
	}

	private static Properties loadDirConfig(final String path) {
		final String userDir = getUseDir();
		final File file1 = new File(userDir + path);
		final Properties properties = new Properties();
		try (FileInputStream in = new FileInputStream(file1);
				final InputStreamReader inputStreamReader = new InputStreamReader(in, UTF8);) {
			properties.load(inputStreamReader);
		} catch (final IOException e) {
			return null;
		}

		return properties;
	}

	private static String getUseDir() {
		return System.getProperty("user.dir");
	}
}
