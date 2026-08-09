package vo.vortex.configuration;

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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;

import vo.vortex.aop.ArgR;
import vo.vortex.cache.STU;
import vo.vortex.scanner.ZPropertiesListener;

/**
 * 读取配置文件
 *
 * @author zhangzhen
 * @date 2025年1月1日 下午10:11:53
 *
 */
public class ZProperties {

	private static final Charset UTF8 = StandardCharsets.UTF_8;

	public static final String PROPERTIES_1 = "config" + File.separator + "application.properties";

	public static final String PROPERTIES_2 = "application.properties";
	public static final String PROPERTIES_NAME = PROPERTIES_2;
	public static final String PROPERTIES_3 = "src" + File.separator + "main" + File.separator + "resources"
			+ File.separator + "application.properties";
	public static final String PROPERTIES_4 = "src" + File.separator + "main" + File.separator + "resources"
			+ File.separator + "config" + File.separator + "application.properties";

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
		if (isRunningFromJar()) {
			final String userDir = getUseDir();

			final File fileCAP = new File(userDir + File.separator + "config" + File.separator + PROPERTIES_NAME);
			if (!fileCAP.exists()) {
				final File fileAP = new File(userDir + File.separator + PROPERTIES_NAME);
				if (!fileAP.exists()) {
					final StringJoiner contentCAPP = loadAP("config" + File.separator + PROPERTIES_NAME);
					if (contentCAPP != null) {
						writeToAp(contentCAPP.toString());
					} else {
						final StringJoiner contentAP = loadAP(PROPERTIES_NAME);
						if (contentAP != null) {
							writeToAp(contentAP.toString());
						} else {

						}
					}
				}
			}
		}
		
		String filePath = getUseDir() + File.separator + ZProperties.PROPERTIES_1;

		Properties p1 = loadDirConfig(File.separator + ZProperties.PROPERTIES_1);
		if (p1 == null) {
			p1 = loadDirConfig(File.separator + ZProperties.PROPERTIES_2);
			filePath = getUseDir() + File.separator + ZProperties.PROPERTIES_2;
			if (p1 == null) {
				p1 = loadPResources("/" + ZProperties.PROPERTIES_1);
				filePath = getUseDir() + File.separator + "src" + File.separator + "main" + File.separator + "resources"
						+ File.separator + ZProperties.PROPERTIES_1;
				if (p1 == null) {
					p1 = loadPResources("/" + ZProperties.PROPERTIES_2);
					filePath = getUseDir() + File.separator + "src" + File.separator + "main" + File.separator + "resources"
							+ File.separator + ZProperties.PROPERTIES_2;
				}
			}
		}

		if (p1 == null) {
			// FIXME 2025年9月1日 上午1:09:02 zhangzhen: 暂时注释,无zf.p则默认用代码中写死的
			p1 = new Properties();
//			System.out.println("ERROR: 启动失败," + ZProperties.PROPERTIES_1 + "配置文件不存在,请编写此配置文件");
//			System.exit(0);
		}

		// FIXME 2025年9月1日 上午2:56:33 zhangzhen: 发现bug：
		// 无app.p文件在linux启动，下面.lis报错：NoSuchFile 所以需要修改此处逻辑，考虑好：
		// 1 无a.p 启动jar，则要同时监控config/a.p 和同目录的a.p 两个文件？还是无a.p启动则指定为config/a.p?
		
		ZPropertiesListener.listen(filePath);
		
		for(final ArgR a : arL) {
			p1.put(a.getKey(), a.getValue());
		}

		properties = p1;
		
		load.set(true);
	}
	
	private static void writeToAp(final String content) {
		
		final String prefix =
				"# 注意：本文件是程序启动时自动生成的，内容是从工程中目录下"
				+ STU.CRLF + "# resources/config/" + PROPERTIES_NAME
				+ STU.CRLF + "# 或 resources/" + PROPERTIES_NAME + " 中拷贝过来的"
				+ STU.CRLF + "# 与工程中的配置完全一致，只为方便查看和修改配置信息"
				+ STU.CRLF + "# 本文件不存在时才自动生成，存在则优先用存在的作为配置"
				+ STU.CRLF + "# 本文件生成时间：" + LocalDateTime.now()
				+ STU.CRLF + STU.CRLF;
		
		final String useDir = getUseDir();
		final File dir = new File(useDir + File.separator + "config");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		final File file = new File(useDir + File.separator + "config" + File.separator + PROPERTIES_NAME);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		FileWriter out = null;
		try {
			out = new FileWriter(file);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		final BufferedWriter writer = new BufferedWriter(out);
		try {
			writer.write(prefix);
			writer.write(content);
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {

			try {
				writer.flush();
				// XXX 为了eclipse中的设置编译通过
				if (out != null) {
					out.flush();
				}
				writer.close();
				if (out != null) {
					out.close();
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static StringJoiner loadAP(final String path) {
		final InputStream inputStream = ZProperties.class.getClassLoader().getResourceAsStream(path);
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
