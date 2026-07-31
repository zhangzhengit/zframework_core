package vo.zframework.route;

import vo.zframework.aop.AOPParameter;

/**
 * AOP 顶级路由接口
 *
 * @author zhangzhen
 * @date 2026年7月18日 14:40:03
 */
public interface IAOPRoute {

	/**
	 * 接口声明，供给子类(当前框架内的子类实现为启动时动态生成)来覆盖，
	 * 然后在指定地方调用本方法，即可实现直接调用，替换当前的反射/方法句柄调用，
	 * 因为当前动态代理类源码直接生成a.b(c,d);的形式，即最普通的方法调用形式
	 *
	 * @param parameter
	 * @return
	 * @throws Exception
	 */
	Object route(AOPParameter parameter) throws Exception;
}
