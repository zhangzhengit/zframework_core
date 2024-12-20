package com.vo.core;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 *
 * @author zhangzhen
 * @date 2024年12月19日 下午4:21:18
 *
 */
@Data
@AllArgsConstructor
public class Fm {

	private final boolean isFormData;
	private final String boundary;

}
