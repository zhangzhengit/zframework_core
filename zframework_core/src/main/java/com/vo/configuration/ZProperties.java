package com.vo.configuration;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.google.common.collect.Lists;
import com.vo.core.ZLog2;
import com.vo.scanner.ZPropertiesListener;

/**
 * zframework.properties 配置文件里全部的k=v的值
 *
 * 加载全部的配置文件的值
 *
 * @author zhangzhen
 * @date 2023年6月29日
 *
 */
public class ZProperties {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static final String UTF_8 = "UTF-8";

	public static final ThreadLocal<String> PROPERTIESCONFIGURATION_ENCODING = new ThreadLocal<>();

	public static final String PROPERTIES_NAME = "zframework.properties";

	public static final String PROPERTIES = "zframework.properties";

	public static final String PROPERTIES_1 = "config/zframework.properties";
	public static final String PROPERTIES_2 = "zframework.properties";
	public static final String PROPERTIES_3 = "src/main/resources/zframework.properties";
	public static final String PROPERTIES_4 = "src/main/resources/config/zframework.properties";

	public static final List<String> PROPERTIES_LIST = Lists.newArrayList(PROPERTIES, PROPERTIES_1, PROPERTIES_2,
			PROPERTIES_3, PROPERTIES_4);

	public static final PropertiesConfiguration P;

	public static PropertiesConfiguration getInstance() {
		return P;
	}

	private ZProperties() {
	}

	static {
		PropertiesConfiguration propertiesConfiguration = null;
		for (final String pv : PROPERTIES_LIST) {
			try {
				propertiesConfiguration = new PropertiesConfiguration(pv);
				propertiesConfiguration.setEncoding(UTF_8);
			} catch (final ConfigurationException e) {
				continue;
			}
		}

		if (propertiesConfiguration == null) {
			LOG.error("找不到配置文件 [{}", ZProperties.PROPERTIES_NAME);
			throw new IllegalArgumentException("找不到配置文件 " + ZProperties.PROPERTIES_NAME);
		}


		final String path = propertiesConfiguration.getPath();
		System.out.println("path = " + path);

		up(propertiesConfiguration, path);

		PROPERTIESCONFIGURATION_ENCODING.set(propertiesConfiguration.getEncoding());
		System.out.println("propertiesConfiguration-encoding = " + propertiesConfiguration.getEncoding());

		ZPropertiesListener.listen(path);

		P = propertiesConfiguration;
	}

	/**
	 *  问题：a.b=这是配置文件里配置的 这个配置使用PropertiesConfiguration获取的结果是=，仅一个=符号。
	 *  所以使用此方法，先使用java.util.Properties.load 配置文件，然后add到PropertiesConfiguration里面。
	 *
	 * @param propertiesConfiguration
	 * @param path
	 *
	 */
	private static void up(final PropertiesConfiguration propertiesConfiguration, final String path) {
		final Properties properties = new Properties();

		try (final FileInputStream fileInputStream = new FileInputStream(path);
				final InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, UTF_8);
				final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);) {
			properties.load(bufferedReader);
		} catch (final IOException e) {
			e.printStackTrace();
		}

		propertiesConfiguration.clear();

		final Set<Object> ks = properties.keySet();
		for (final Object k : ks) {
			propertiesConfiguration.addProperty(String.valueOf(k), properties.get(k));
		}
	}

}
