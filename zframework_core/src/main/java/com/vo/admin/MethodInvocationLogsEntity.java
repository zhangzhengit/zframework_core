package com.vo.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 *
 * @author zhangzhen
 * @date 2024年5月31日 下午2:42:37
 * 
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
// FIXME 2024年5月31日 下午5:59:02 zhangzhen : 要在启动时create 此表，如果不存在就create
// 并提供一个配置项：启动就重建table、启动时不存在此table才重建 等等
// 由于是在zf工程中执行create语句，所以re要再提供一个执行create语句的方法
//@ZEntity(tableName = "method_invocation_logs", dataSourceName = "zdatasource-sqlite.properties")
public class MethodInvocationLogsEntity {

	//	@ZID
	private Integer id;

	/**
	 * 方法(API方法)名称
	 */
	private String methodName;

	/**
	 * 方法执行耗时
	 */
	private Integer timeConsuming;

}
