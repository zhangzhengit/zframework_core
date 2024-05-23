package com.vo.http;

import com.vo.anno.ZConfigurationProperties;
import com.vo.anno.ZValue;
import com.vo.validator.ZMax;
import com.vo.validator.ZMin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ZRequestMapping 的配置类
 *
 * @author zhangzhen
 * @date 2023年11月3日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZConfigurationProperties(prefix = "request.mapping")
public class ZRequestMappingConfigurationProperties {

	public static final int MIN_VALUE = 100;
	
	public static final int MAX_VALUE = 10000 * 20;

	public static final int DEFAULT_VALUE = 10000 * 5;

	@ZMin(min = MIN_VALUE)
	@ZMax(max = MAX_VALUE)
	// FIXME 2023年11月3日 下午9:58:24 zhanghen: TODO 此类作为基础配置类
	//加入下一行 @ZValue实时更新，只做到了 实时更新ZRequestMappingConf对象，
	// 使用此基础配置类时是在启动时就初始化好了的，没有在使用时每次都在本对象中取值，
	// 所以 做不到 实时更新 @ZRequestMapping 的qps默认值。
	// 如果要做到，需要修改 存取 @ZRequestMapping.qps的方法逻辑
//	@ZValue(name = "request.mapping.qps", listenForChanges = true)
	private Integer qps = DEFAULT_VALUE;

}
