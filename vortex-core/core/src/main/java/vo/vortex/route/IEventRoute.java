package vo.vortex.route;

/**
 * 事件路由接口
 *
 * @author zhangzhen
 * @date 2026年7月16日 09:02:11
 */
public interface IEventRoute {

	void route(final vo.vortex.event.ZApplicationEvent event);

}
