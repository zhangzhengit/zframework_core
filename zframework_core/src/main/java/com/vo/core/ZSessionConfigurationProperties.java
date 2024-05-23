package com.vo.core;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZMax;
import com.vo.validator.ZMin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ZSession 存储配置
 *
 * @author zhangzhen
 * @date 2023年11月27日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZConfigurationProperties(prefix = "session")
public class ZSessionConfigurationProperties {

	/**
	 * 存储到ZMap时的分组数
	 */
	@ZMin(min = 1000)
	@ZMax(max = 10000 * 100)
	private Integer groups = 1000;

	/**
	 * 存储到ZMap时的每个组的最大个数
	 */
	@ZMin(min = 10)
	@ZMax(max = 1000)
	private Integer numberOfGroup = 100;

}
