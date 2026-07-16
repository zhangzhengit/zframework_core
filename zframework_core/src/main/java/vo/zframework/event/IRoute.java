package vo.zframework.event;

/**
 * 事件路由接口
 *
 * @author zhangzhen
 * @date 2026年7月16日 09:02:11
 */
public interface IRoute {

	void route(final vo.zframework.event.ZApplicationEvent event);

}
