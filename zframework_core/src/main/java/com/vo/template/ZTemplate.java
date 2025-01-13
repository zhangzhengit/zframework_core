package com.vo.template;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.function.Supplier;

import com.vo.configuration.ServerConfigurationProperties;
import com.vo.core.ZContext;
import com.vo.core.ZRC;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * freemarker模板工具类
 *
 * @author zhangzhen
 * @date 2023年6月27日
 *
 */
public class ZTemplate {

	private static final Configuration CFG = new Configuration();

	/**
	 * 从一个带有freemarker标签的html文档的字符串形式，处理其中的freemarker标签，而生成一个完整的可以被浏览器直接解析的html文档。
	 *
	 * @param htmlContent
	 * @return
	 *
	 */
	public static String freemarker(final String htmlContent) {
		return freemarker0(htmlContent);
	}

	static {

		final ServerConfigurationProperties serverConfigurationProperties = ZContext.getBean(ServerConfigurationProperties.class);
		final String staticPrefix = serverConfigurationProperties.getStaticPrefix();
		CFG.setClassForTemplateLoading(ZTemplate.class, staticPrefix);

	}

	private static String freemarker0(final String templateString) {
		final Map<String, Object> dataModel = ZModel.get();

		final String key =
				"f0-" + dataModel.hashCode()
				+ "-" + dataModel.size()
				+ "-" + templateString;

		final String v = ZRC.computeIfAbsent(key, () ->{

			try {
				final Template template = getTemplate(templateString);

				final StringWriter writer = new StringWriter();

				template.process(dataModel, writer);
				final String output = writer.toString();
				return output;
			} catch (IOException | TemplateException e) {
				e.printStackTrace();
			}

			return null;
		});

		return v;
	}

	private static Template getTemplate(final String templateString) throws IOException {
		return ZRC.computeIfAbsent(templateString, supplier(templateString));
	}

	private static Supplier<Template> supplier(final String templateString) {
		return () -> {
			try {
				return new Template("template-" + templateString.hashCode(), templateString, CFG);
			} catch (final IOException e) {
				e.printStackTrace();
			}
			return null;
		};
	}

}
