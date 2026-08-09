package vo.vortex.configuration;

import vo.vortex.anno.ZBean;
import vo.vortex.anno.ZConfiguration;
import vo.vortex.anno.ZOrder;
import vo.vortex.cache.ZCache;
import vo.vortex.cache.ZCacheMemory;
import vo.vortex.cache.ZCacheR;

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
