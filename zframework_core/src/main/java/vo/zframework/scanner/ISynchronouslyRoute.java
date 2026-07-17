package vo.zframework.scanner;

import vo.zframework.aop.AOPParameter;
import vo.zframework.event.APIRouteR;

/**
 * @ZSynchronously 路由
 *
 * @author zhangzhen
 * @date 2026年7月17日 19:56:18
 */
public interface ISynchronouslyRoute {

	/**
	 * @param parameter
	 * @return
	 * @throws Exception
	 */
	APIRouteR route(AOPParameter parameter) throws Exception;

}
