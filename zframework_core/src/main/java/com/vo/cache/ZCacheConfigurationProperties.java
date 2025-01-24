package com.vo.cache;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZNotEmtpy;
import com.vo.validator.ZNotNull;

/**
 * cache配置项
 *
 * @author zhangzhen
 * @date 2023年11月5日
 *
 */
@ZConfigurationProperties(prefix = "cache")
public class ZCacheConfigurationProperties {

	/**
	 * 配置缓存类型 @see ZCacheConfiguration 中的常亮
	 */
	@ZNotEmtpy
	private String type = ZCacheConfiguration.DEFAULT;

	/**
	 * 配置是否启用 cache 缓存功能
	 */
	@ZNotNull
	private Boolean enable = true;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Boolean getEnable() {
		return enable;
	}

	public void setEnable(Boolean enable) {
		this.enable = enable;
	}
	
	
}
