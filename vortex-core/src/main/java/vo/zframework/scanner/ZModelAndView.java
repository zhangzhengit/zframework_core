package vo.zframework.scanner;

import java.util.Map;

import vo.zframework.template.ZModel;

/**
 * @ZHtml 接口返回的视图名称和数据
 *
 * @author zhangzhen
 * @date 2023年11月15日
 *
 */
public class ZModelAndView {

	/**
	 * 目标方法是否带有 @ZHtml 注解（是否返回html页面）
	 */
	private final boolean isHtml;

	/**
	 * html名称
	 */
	private final String htmlName;

	/**
	 * html内容，还没替换freemarker标签的原始内容
	 */
	private final String htmlContent;

	/**
	 * html中对应的数据
	 */
	private final Map<String, Object> map;

	/**
	 * html 目标方法中用于向html中传输参数值的 ZModel参数对象
	 */
	private final ZModel model;

	/**
	 * 【非】返回html页面的方法的返回值
	 */
	private final Object object;

	public boolean isHtml() {
		return isHtml;
	}

	public String getHtmlName() {
		return htmlName;
	}

	public String getHtmlContent() {
		return htmlContent;
	}

	public Map<String, Object> getMap() {
		return map;
	}

	public ZModel getModel() {
		return model;
	}

	public Object getObject() {
		return object;
	}

	public ZModelAndView(boolean isHtml, String htmlName, String htmlContent, Map<String, Object> map, ZModel model,
			Object object) {
		super();
		this.isHtml = isHtml;
		this.htmlName = htmlName;
		this.htmlContent = htmlContent;
		this.map = map;
		this.model = model;
		this.object = object;
	}

}
