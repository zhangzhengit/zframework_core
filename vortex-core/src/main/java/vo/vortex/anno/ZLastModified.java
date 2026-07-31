package vo.vortex.anno;

/**
 * 用在接口方法上，表示此接口响应header中添加 Last-Modified
 *
 * @author zhangzhen
 * @date 2025年1月3日 上午3:26:34
 *
 */
//@Documented
//@Retention(RetentionPolicy.RUNTIME)
//@Target({ ElementType.METHOD })
// FIXME 2026年6月24日 14:59:44 zhangzhen : 此注解应该删除了，先不删除，注释了
// 留着此文件 提示自己，曾思考过要不要和@ZETag一样支持放在接口上，但后来觉得不好统一处理，只在类似
// @see StaticController 处理静态文件时Last-Modified 头容易统一处理，并且已经有了ETag了
// 这个头继续支持的意义不大了。
public @interface ZLastModified {

}
