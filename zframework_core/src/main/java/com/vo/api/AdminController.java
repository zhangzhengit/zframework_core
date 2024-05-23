package com.vo.api;

import com.vo.anno.ZConfiguration;
import com.vo.anno.ZController;
import com.vo.http.ZRequestMapping;

/**
 * 管理后台的接口
 *
 * @author zhangzhen
 * @date 2023年7月12日
 *
 */
// FIXME 2023年7月12日 下午12:30:38 zhanghen: TODO 使用用户名密码登录（见配置），可以查看运行时间
// 各方法调用次数、执行时长、耗时排序、执行异常信息、可用内存等信息
@ZController
public class AdminController {


	@ZRequestMapping(mapping = { "/admin" }, qps = 1000)
	public String adminIndex() {
		return "";
	}
}
