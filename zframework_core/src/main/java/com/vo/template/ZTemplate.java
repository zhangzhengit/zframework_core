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
	 * 从一个带有freemarker标签的html文档的字符串形式，
	 * 处理其中的freemarker标签，而生成一个完整的可以被浏览器直接解析的html文档。
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

		// FIXME 2025年11月26日 12:03:26 zhangzhen :  暂时的修改：
		// 1的方式对于动态生成的页面，如带有时间戳的页面，会一直缓存导致浪费大量内存
		// 暂时想到的办法：
		// 1 加入缓存过期策略，似乎也在大多场景下没啥意义，因为缓存复用的可能不太大
		// 2 如下方式，直接不用缓存
		try (StringWriter writer = new StringWriter()) {
			final Template template = getTemplateCache(templateString);

			template.process(dataModel, writer);
			return writer.toString();
		} catch (IOException | TemplateException e) {
			e.printStackTrace();
		}
		return null;
		
		// 1 用cache，导致占了太多内存了
//		final String key =
//				dataModel != null ?
//						("f0-" + dataModel.hashCode()
//						+ "-" + dataModel.size()
//						+ "-" + templateString) :
//							templateString;
//
//		return ZRC.computeIfAbsent("freemarker0" + '-' + key, () ->{
//
//			try (StringWriter writer = new StringWriter()) {
//				final Template template = getTemplateCache(templateString);
//
//				template.process(dataModel, writer);
//				return writer.toString();
//			} catch (IOException | TemplateException e) {
//				e.printStackTrace();
//			}
//
//			return null;
//		});
	}

	private static Template getTemplateCache(final String templateString) throws IOException {
		return ZRC.computeIfAbsent("fT-" + templateString, supplier(templateString));
	}

	private static Supplier<Template> supplier(final String templateString) {
		return () -> {
			try {
				return new Template("fT-" + templateString.hashCode(), templateString, CFG);
			} catch (final IOException e) {
				e.printStackTrace();
			}
			return null;
		};
	}

}
