package com.vo.apidoc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @author zhangzhen
 * @date 2024年12月17日 下午6:32:16
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class APIInfo {

	/**
	 * API描述信息
	 */
	private String description;

	/**
	 * http METHOD
	 */
	private String method;

	/**
	 * path
	 */
	private String mapping;

	/**
	 * 参数
	 */
	private String param;

}
