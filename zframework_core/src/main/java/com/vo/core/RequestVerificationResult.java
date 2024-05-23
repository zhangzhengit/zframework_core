package com.vo.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @author zhangzhen
 * @date 2024年2月16日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestVerificationResult {

	/**
	 * 是否校验通过
	 */
	private boolean passed;

	/**
	 * 提示信息
	 */
	private String message;

}
