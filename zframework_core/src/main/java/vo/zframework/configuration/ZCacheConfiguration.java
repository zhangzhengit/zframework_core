package vo.zframework.configuration;

import vo.zframework.anno.ZBean;
import vo.zframework.anno.ZConfiguration;
import vo.zframework.anno.ZOrder;
import vo.zframework.cache.ZCache;
import vo.zframework.cache.ZCacheMemory;
import vo.zframework.cache.ZCacheR;

/**
 *	Cache配置
 *
 * @author zhangzhen
 * @date 2023年11月5日
 *
 */
@ZConfiguration
@ZOrder(value = Integer.MIN_VALUE)
public class ZCacheConfiguration {

	@ZBean
	public ZCache<ZCacheR> cacheBbuiltinForPackageCache() {
		return new ZCacheMemory();
	}

}
