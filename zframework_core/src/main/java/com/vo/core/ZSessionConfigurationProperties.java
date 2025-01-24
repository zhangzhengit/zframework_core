package com.vo.core;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZMax;
import com.vo.validator.ZMin;

/**
 * ZSession 存储配置
 *
 * @author zhangzhen
 * @date 2023年11月27日
 *
 */
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

	public Integer getGroups() {
		return groups;
	}

	public void setGroups(Integer groups) {
		this.groups = groups;
	}

	public Integer getNumberOfGroup() {
		return numberOfGroup;
	}

	public void setNumberOfGroup(Integer numberOfGroup) {
		this.numberOfGroup = numberOfGroup;
	}

	public ZSessionConfigurationProperties(Integer groups, Integer numberOfGroup) {
		super();
		this.groups = groups;
		this.numberOfGroup = numberOfGroup;
	}
	
	public ZSessionConfigurationProperties() {
	}

}
