package vo.zframework.template;

/**
 *
 * 模板渲染
 *
 * @author zhangzhen
 * @date 2026年6月24日 17:24:02
 */
public interface ZTemplateEngine {

	/**
	 * 渲染
	 *
	 * @param htmlName
	 * @param htmlContent
	 * @return
	 */
	String render(final String htmlName, final String htmlContent);

}
