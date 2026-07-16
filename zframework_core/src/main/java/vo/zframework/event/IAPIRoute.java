package vo.zframework.event;

import vo.zframework.http.ZRMethod;

/**
 * 接口方法路由
 *
 * @author zhangzhen
 * @date 2026年7月16日 10:00:00
 */
public interface IAPIRoute {

	/**
	 * @param path       请求的path
	 * @param controller 目标方法所在的对象
	 * @param zrMethod   真正要执行的目标方法
	 * @param parameters 方法参数
	 * @return
	 */
	APIRouteR route(String path, Object controller, ZRMethod zrMethod, Object[] parameters) throws Exception;

}
