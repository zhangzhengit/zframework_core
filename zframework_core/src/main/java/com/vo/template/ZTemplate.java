package com.vo.template;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

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
//	private static final Configuration CFG = new Configuration(Configuration.VERSION_2_3_31);

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

	private static String freemarker0(final String templateString) {

		try {

			// FIXME 2024年2月13日 下午5:29:45 zhanghen: 目录不能在此写死，改为配置项，并且考虑好如果配置了读取硬盘上的
			// 的静态文件（包括html）时，是否优先使用静态文件的目录
			CFG.setClassForTemplateLoading(ZTemplate.class, "/static");

			final Template template = new Template("template-" + templateString.hashCode(), templateString, CFG);
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

}
