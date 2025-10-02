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
	private int groups = 1000;

	/**
	 * 存储到ZMap时的每个组的最大个数
	 */
	@ZMin(min = 10)
	@ZMax(max = 1000)
	private int numberOfGroup = 100;

	public int getGroups() {
		return this.groups;
	}

	public void setGroups(final int groups) {
		this.groups = groups;
	}

	public int getNumberOfGroup() {
		return this.numberOfGroup;
	}

	public void setNumberOfGroup(final int numberOfGroup) {
		this.numberOfGroup = numberOfGroup;
	}

	public ZSessionConfigurationProperties(final int groups, final int numberOfGroup) {
		this.groups = groups;
		this.numberOfGroup = numberOfGroup;
	}
	
	public ZSessionConfigurationProperties() {
	}

}
