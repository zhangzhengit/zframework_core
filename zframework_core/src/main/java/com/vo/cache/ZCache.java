package com.vo.cache;

import java.util.Set;

/**
 * 缓存接口，不精确的缓存实现，只用于近似的获取数据，如果获取的数据需要insert、update等，则还需要再次判断
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
public interface ZCache<V> {

	/**
	 * 	ZCacheConfiguration.cacheBbuiltinForPackageCache 返回的缓存Bean的名称，使用 此名称来注入配置的内置的缓存类。
	 * 	如下即可注入配置好的缓存类对象

  		@ZAutowired(name = ZCache.CACHE_BBUILTIN_FOR_PACKAGE_CACHE)
		private ZCache<ZCacheR> cache;

	 */
	public static final String CACHE_BBUILTIN_FOR_PACKAGE_CACHE = "cacheBbuiltinForPackageCache";

	public void add(final String key, final V value, long expire);

	public V get(final String key);

	public void remove(final String key);

	public boolean contains(final String key);

	public Set<String> keySet();

}
