package com.vo.admin;

import com.vo.anno.ZController;
import com.vo.http.ZHtml;
import com.vo.http.ZRequestMapping;
import com.votool.common.CR;

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

	//	@ZAutowired
	//	private MethodInvocationLogsRepository methodInvocationLogsRepository;

	@ZHtml
	@ZRequestMapping(mapping = { "/admin" }, qps = ZRequestMapping.MIN_QPS)
	public String adminIndex() {

		//		final MethodInvocationLogsEntity p = new MethodInvocationLogsEntity();
		//		final Sort sort = Sort.create().descendingBy("id");
		//		final Page<MethodInvocationLogsEntity> page = methodInvocationLogsRepository.page(p, sort, 1, 20);
		//		return CR.ok(page);


		//		return CR.ok();
		return "html/admin.html";
	}
}
