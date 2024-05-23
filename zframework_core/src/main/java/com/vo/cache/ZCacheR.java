package com.vo.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 缓存的对象
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ZCacheR {

	private String key;
	private Object value;

	/**
	 * 过期毫秒数
	 */
	private long expire;

	/**
	 * 新建此对象的时间戳
	 */
	private long currentTimeMillis;
}
