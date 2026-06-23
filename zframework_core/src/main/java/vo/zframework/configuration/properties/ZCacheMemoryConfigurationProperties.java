package vo.zframework.configuration.properties;

import vo.zframework.anno.ZMax;
import vo.zframework.anno.ZMin;

/**
 * 内存缓存相关配置
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@ZConfigurationProperties(prefix = "cache.memory")
// FIXME 2025年12月20日 07:06:09 zhangzhen :  和@ZCache几个注解仔细考虑好，timeout设置多少合适
// 并且注解值不得大于此值，大则提示修改其一
public class ZCacheMemoryConfigurationProperties {

	/**
	 * 最大超时时间[秒]，不是具体哪个缓存值得超时时间。超过此值自动删除
	 */
	@ZMin(min = 60 * 60)
	@ZMax(max = 60 * 60 * 24 * 30)
	private int maxTimeout = 60 * 60 * 24 * 1;
	
	/**
	 * 缓存容量，超过此值则自动淘汰最近最少访问的
	 */
	@ZMin(min = 1)
	@ZMax(max = 10000 * 500)
	private int capacity = 10000 * 10;
	
	public int getMaxTimeout() {
		return this.maxTimeout;
	}
	
	public void setMaxTimeout(final int sessionMaxTimeout) {
		this.maxTimeout = sessionMaxTimeout;
	}

	public int getCapacity() {
		return this.capacity;
	}

	public void setCapacity(final int capacity) {
		this.capacity = capacity;
	}

	@Override
	public String toString() {
		return "ZCacheMemoryConfigurationProperties [maxTimeout=" + this.maxTimeout + ", capacity=" + this.capacity + "]";
	}

}
