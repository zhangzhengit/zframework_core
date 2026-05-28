package com.vo.zframework.configuration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;

import com.vo.zframework.aop.ArgR;
import com.vo.zframework.cache.STU;
import com.vo.zframework.scanner.ZPropertiesListener;

/**
 * 读取配置文件
 *
 * @author zhangzhen
 * @date 2025年1月1日 下午10:11:53
 *
 */
public class ZProperties {

	private static final String MAIN = "main";

	private static final String SRC = "src";

	private static final String CONFIG = "config";

	public static final String DEFALUT_PROPERTIES_NAME = "application.properties";

	public static final String CONFIG_PROPERTIES_NAME = CONFIG + File.separator + "application.properties";


	public static final String SRC_MAIN_RESOURCES_PROPERTIES_NAME =
			SRC + File.separator + MAIN + File.separator + "resources"
			+ File.separator + DEFALUT_PROPERTIES_NAME;

	public static final String SRC_MAIN_RESOURCES_CONFIG_PROPERTIES_NAME =
			SRC + File.separator + MAIN + File.separator + "resources"
			+ File.separator + CONFIG + File.separator + DEFALUT_PROPERTIES_NAME;

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
		if (!STU.hasContent(v)) {
			return null;
		}

		if (AppH.isExpression(v)) {
			final Object r = EE.execute(AppH.gExpression(v));
			return Byte.parseByte(String.valueOf(r));
		}

		return Byte.parseByte(v);
	}

	public static Short getShort(final String key) {
		final String v = properties.getProperty(key);
		if (!STU.hasContent(v)) {
			return null;
		}

		if (AppH.isExpression(v)) {
			final Object r = EE.execute(AppH.gExpression(v));
			return Short.parseShort(String.valueOf(r));
		}

		return Short.parseShort(v);
	}

	public static Integer getInteger(final String key, final Integer defaultValue) {
		final String v = properties.getProperty(key);
		if (!STU.hasContent(v)) {
			return defaultValue;
		}

		if (AppH.isExpression(v)) {
			final Object r = EE.execute(AppH.gExpression(v));
			return Integer.parseInt(String.valueOf(r));
		}

		return Integer.parseInt(v);
	}

	public static Integer getInteger(final String key) {
		final String v = properties.getProperty(key);
		if (!STU.hasContent(v)) {
			return null;
		}

		// FIXME 2025年12月26日 18:30:18 zhangzhen :  继续支持其他的
		if (AppH.isExpression(v)) {
			final Object r = EE.execute(AppH.gExpression(v));
			return Integer.parseInt(String.valueOf(r));
		}

		return Integer.parseInt(v);
	}

	public static Long getLong(final String key) {
		final String v = properties.getProperty(key);
		if (!STU.hasContent(v)) {
			return null;
		}

		if (AppH.isExpression(v)) {
			final Object r = EE.execute(AppH.gExpression(v));
			return Long.parseLong(String.valueOf(r));
		}

		return Long.parseLong(v);
	}

	public static BigInteger getBigInteger(final String key) {
		final String v = properties.getProperty(key);
		if (!STU.hasContent(v)) {
			return null;
		}

		if (AppH.isExpression(v)) {
			final Object r = EE.execute(AppH.gExpression(v));
			return new BigInteger(String.valueOf(r));
		}

		return new BigInteger(v);
	}

	public static BigDecimal getBigDecimal(final String key) {
		final String v = properties.getProperty(key);
		if (!STU.hasContent(v)) {
			return null;
		}

		if (AppH.isExpression(v)) {
			final Object r = EE.execute(AppH.gExpression(v));
			return new BigDecimal(String.valueOf(r));
		}

		return new BigDecimal(v);
	}

	public static Float getFloat(final String key) {
		final String v = properties.getProperty(key);
		if (!STU.hasContent(v)) {
			return null;
		}

		if (AppH.isExpression(v)) {
			final Object r = EE.execute(AppH.gExpression(v));
			return Float.parseFloat(String.valueOf(r));
		}

		return Float.parseFloat(v);
	}
	public static Double getDouble(final String key) {
		final String v = properties.getProperty(key);
		if (!STU.hasContent(v)) {
			return null;
		}

		if (AppH.isExpression(v)) {
			final Object r = EE.execute(AppH.gExpression(v));
			return Double.parseDouble(String.valueOf(r));
		}

		return Double.parseDouble(v);
	}

	public static Boolean getBoolean(final String key) {
		final String v = properties.getProperty(key);
		if (!STU.hasContent(v)) {
			return null;
		}

		if (AppH.isExpression(v)) {
			final Object r = EE.execute(AppH.gExpression(v));
			return Boolean.parseBoolean(String.valueOf(r));
		}

		return Boolean.parseBoolean(v);
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

	public static String getString(final String key) {
		final String v = properties.getProperty(key);
		if (v == null) {
			return null;
		}

		if (AppH.isExpression(v)) {
			final Object r = EE.execute(AppH.gExpression(v));
			return String.valueOf(r);
		}

		return v;
	}

	public static String[] getStringArray(final String key) {
		final Object v = properties.get(key);
		if (v == null) {
			return EMPTY_STRING_ARRAY;
		}

		if (AppH.isExpression(String.valueOf(v))) {
			final Object r = EE.execute(AppH.gExpression(String.valueOf(v)));
			final String s1 = String.valueOf(r);
			return s1.split(",");
		}

		final String s1 = String.valueOf(v);
		final String[] a = s1.split(",");
		return a;
	}


	private static Properties properties;

	public static Properties getInstance() {
		return properties;
	}

	public static List<ArgR> arL = new ArrayList<>();
	private static AtomicBoolean load = new AtomicBoolean(false);

	public synchronized static void load() {

		if (load.get()) {
			return;
		}

		// jar方式运行时，如果不存在config/a.p和当前目录下的a.p，则读取jar中的配置文件，写入到到config/a.p
		// 如果这4种方式都不存在a.p，则什么也不做
		copyAPToConfigWhenRunningJar();

		String filePath = getUseDir() + File.separator + ZProperties.CONFIG_PROPERTIES_NAME;

		Properties p1 = null;
		if (isRunningFromJar()) {
			p1 = loadDirConfig(File.separator + ZProperties.CONFIG_PROPERTIES_NAME);
			if (p1 == null) {
				p1 = loadDirConfig(File.separator + "application.properties");
				if (p1 == null) {
					p1 = loadPResources("/" + ZProperties.CONFIG_PROPERTIES_NAME);
					if (p1 == null) {
						p1 = loadPResources("/" + "application.properties");
					}
				} else {
					filePath = getUseDir() + File.separator + "application.properties";
				}
			} else {
				filePath = getUseDir() + File.separator + ZProperties.CONFIG_PROPERTIES_NAME;
			}
		} else {
			p1 = loadPResources("/" + ZProperties.CONFIG_PROPERTIES_NAME);
			filePath = getUseDir() + File.separator + SRC + File.separator + MAIN + File.separator + "resources"
					+ File.separator + ZProperties.CONFIG_PROPERTIES_NAME;
			if (p1 == null) {
				p1 = loadPResources("/" + "application.properties");
				filePath = getUseDir() + File.separator + SRC + File.separator + MAIN + File.separator + "resources"
						+ File.separator + "application.properties";
			}
		}

		ZPropertiesListener.listen(filePath);

		if (p1 == null) {
			// 到此，无 app.p配置文件，程序仍可以正常启动运行(支持0配置启动)
			// 但为了下面的存放命令行参数
			// 和可能的NPE，在此赋值为 new Properties
			p1 = new Properties();
		}

		for(final ArgR a : arL) {
			p1.put(a.getKey(), a.getValue());
		}

		properties = p1;

		load.set(true);
	}

	/**
	 * 以.jar方式运行，并且config下和当前目录都没有application.properties 时，
	 * 则把.jar中的resources/config下的或者resources下的application.properties
	 * 复制一份到当前目录的config目录下
	 */
	private static void copyAPToConfigWhenRunningJar() {

		if (!isRunningFromJar()) {
			return;
		}

		final String userDir = getUseDir();

		final File fileResourcesConfigAP = new File(userDir + File.separator + CONFIG + File.separator + DEFALUT_PROPERTIES_NAME);
		if (!fileResourcesConfigAP.exists()) {
			final File fileResourcesAP = new File(userDir + File.separator + DEFALUT_PROPERTIES_NAME);
			if (!fileResourcesAP.exists()) {
				// config/app.p 和 app.p 都不存在，则从jar中config/app.p或者app.p 复制一份到config/app.p
				final StringJoiner apContent = loadAPFromJar(CONFIG + "/" + DEFALUT_PROPERTIES_NAME);
				if (apContent != null) {
					writeToConfigAp(apContent.toString());
				} else {
					final StringJoiner contentAP = loadAPFromJar(DEFALUT_PROPERTIES_NAME);
					if (contentAP != null) {
						writeToConfigAp(contentAP.toString());
					}
				}
			}
		}
	}

	private static void writeToConfigAp(final String content) {

		final String prefix =
				"# 注意：本文件是程序启动时自动生成的，内容是从工程中目录下"
				+ STU.CRLF + "# resources/config/" + DEFALUT_PROPERTIES_NAME
				+ STU.CRLF + "# 或 resources/" + DEFALUT_PROPERTIES_NAME + " 中拷贝过来的"
				+ STU.CRLF + "# 与工程中的配置完全一致，只为方便查看和修改配置信息"
				+ STU.CRLF + "# 本文件不存在时才自动生成，存在则优先用存在的作为配置"
				+ STU.CRLF + "# 本文件生成时间：" + LocalDateTime.now()
				+ STU.CRLF + STU.CRLF;

		final String useDir = getUseDir();

		final File file = mkdirConfigApp(useDir);

		try (FileWriter out = new FileWriter(file);
			BufferedWriter writer = new BufferedWriter(out)) {
			writer.write(prefix);
			writer.write(content);
			writer.flush();
		} catch (final IOException e) {
			e.printStackTrace();
		}

	}

	private static File mkdirConfigApp(final String useDir) {
		final File dir = new File(useDir + File.separator + CONFIG);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		final File file = new File(useDir + File.separator + CONFIG + File.separator + DEFALUT_PROPERTIES_NAME);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		return file;
	}

	private static StringJoiner loadAPFromJar(final String path) {

		final InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
		if (inputStream == null) {
			return null;
		}

		final StringJoiner joiner = new StringJoiner(STU.CRLF);

		final InputStreamReader in = new InputStreamReader(inputStream);
		final BufferedReader reader = new BufferedReader(in);
		while (true) {
			try {
				final String readLine = reader.readLine();
				if (readLine == null) {
					break;
				}
				joiner.add(readLine);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		try {
			reader.close();
			in.close();
			inputStream.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return joiner;
	}

	private static boolean isRunningFromJar() {
		final String location = ZProperties.class.getProtectionDomain().getCodeSource().getLocation().toString();
		return location.startsWith("jar:") && location.contains(".jar");
	}

	private static Properties loadPResources(final String path) {
		final InputStream inputStream = ZProperties.class.getResourceAsStream(path);
		if (inputStream == null) {
			return null;
		}

		final Properties p2 = new Properties();
		InputStreamReader reader = null;
		try {
			reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
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
		final File file = new File(userDir + path);
		final Properties properties = new Properties();
		try (FileInputStream in = new FileInputStream(file);
				final InputStreamReader inputStreamReader = new InputStreamReader(in, StandardCharsets.UTF_8);) {
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
