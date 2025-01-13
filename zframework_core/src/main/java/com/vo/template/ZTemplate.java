package com.vo.template;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.function.Supplier;

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

		// FIXME 2024年2月13日 下午5:29:45 zhanghen: 目录不能在此写死，改为配置项，并且考虑好如果配置了读取硬盘上的
		// 的静态文件（包括html）时，是否优先使用静态文件的目录
		CFG.setClassForTemplateLoading(ZTemplate.class, "/static");

	}

	private static String freemarker0(final String templateString) {

		try {
			final Template template = getTemplate(templateString);

			final StringWriter writer = new StringWriter();
			final Map<String, Object> dataModel = ZModel.get();
			template.process(dataModel, writer);
			final String output = writer.toString();
			return output;
		} catch (IOException | TemplateException e) {
			e.printStackTrace();
		}

		return templateString;
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
