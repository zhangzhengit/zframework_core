package vo.zframework.template;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.function.Supplier;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import vo.zframework.configuration.ServerConfigurationProperties;
import vo.zframework.core.ZContext;
import vo.zframework.core.ZRC;

/**
 * freemarker模板工具类
 *
 * @author zhangzhen
 * @date 2023年6月27日
 *
 */
public class ZTemplate {

	private static final Configuration CFG = new Configuration();

	private static final int C = 500;
	
	/**
	 * 	这个对象是单独为freemarker而设立的，之前发现：如用共同的可能导致缓存过多的页面又几乎用不到而耗费太大内存
	 * 	但对于少部分会复用的页面(渲染出来几乎不变的页面)，每次都process又太耗时间，所以单独在此提供一个
	 *  小容量的缓存
	 */
	private static final ZRC F = new ZRC(C);

	/**
	 * 从一个带有freemarker标签的html文档的字符串形式，
	 * 处理其中的freemarker标签，而生成一个完整的可以被浏览器直接解析的html文档。
	 * @param htmlName TODO
	 * @param htmlContent
	 *
	 * @return
	 *
	 */
	public static String freemarker(final String htmlName, final String htmlContent) {
		return freemarker0(htmlName, htmlContent);
	}

	static {

		final ServerConfigurationProperties serverConfigurationProperties = ZContext.getBean(ServerConfigurationProperties.class);
		final String staticPrefix = serverConfigurationProperties.getStaticPrefix();
		CFG.setClassForTemplateLoading(ZTemplate.class, staticPrefix);

	}

	private static String freemarker0(final String htmlName, final String templateString) {
		final Map<String, Object> dataModel = ZModel.get();

		final String key =
				dataModel != null ?
						("f0-" + dataModel.hashCode()
						+ '-' + dataModel.size()
						+ '-' + htmlName.length()
						+ '-' + htmlName
								) :
							templateString;

		return F.computeIfAbsent("f1" + '-' + key, () ->{

			try (StringWriter writer = new StringWriter()) {
				final Template template = getTemplateCache(templateString);

				template.process(dataModel, writer);
				return writer.toString();
			} catch (IOException | TemplateException e) {
				e.printStackTrace();
			}

			return null;
		});
	}

	private static Template getTemplateCache(final String templateString) throws IOException {
		return ZRC.singleton().computeIfAbsent("fTC-" + templateString, supplier(templateString));
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
