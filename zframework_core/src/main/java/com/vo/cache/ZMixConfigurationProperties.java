package com.vo.cache;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZMax;
import com.vo.validator.ZMin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 为 ZMix 缓存类的配置类
 *
 * @author zhangzhen
 * @date 2023年11月8日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZConfigurationProperties(prefix = "cache.type.mix")
public class ZMixConfigurationProperties {

	// FIXME 2023年11月8日 下午7:12:52 zhanghen: XXX 是否校验cache.type=MIX 下
	// cache.type.mix.expire 要小于所有的 @ZCacheable.expire?

	/**
	 * 配置内存超时毫秒数,使用Byte类型，因为此值必须配置一个很小的值，如果配置很大，超过了@ZCacheable.expire则内存缓存会晚于redis缓存超时，显然是不合理的.
	 * 此属相专门配置内存超时，使用MIX类型时此类才会被用到.
	 */
	@ZMin(min = 1)
	@ZMax(max = Byte.MAX_VALUE)
	private Byte memoryExpire = 50;

}
