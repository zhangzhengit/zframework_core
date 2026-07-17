package vo.zframework.scanner;

import vo.zframework.aop.AOPParameter;

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
	Object route(AOPParameter parameter) throws Exception;

}
