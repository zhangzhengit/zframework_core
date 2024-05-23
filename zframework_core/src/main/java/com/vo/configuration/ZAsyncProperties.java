package com.vo.configuration;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZMax;
import com.vo.validator.ZMin;
import com.vo.validator.ZNotEmtpy;
import com.vo.validator.ZNotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ZAsync 用到的线程池的相关配置
 *
 * @author zhangzhen
 * @date 2023年7月8日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZConfigurationProperties(prefix = "async")
public class ZAsyncProperties {

	/**
	 * 最大线程数量
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = Integer.MAX_VALUE)
	private Integer threadCount = Runtime.getRuntime().availableProcessors();

	/**
	 * 线程名称前缀
	 */
	@ZNotNull
	@ZNotEmtpy
	// FIXME 2023年11月3日 下午10:04:55 zhanghen: XXX
	// @ZConfigurationProperties 的基础配置类到底要不要 使用
	// @ZValue来实时更新？如本类，如果更新了线程数量，那么异步线程池如何处理？
	// 是废弃原线程池然后新建一个线程池？还是对原线程池增减线程？
	// @ZValue 要不要 新建一个方法属性，指定一个方法，来实现listenForChanges = true
	// 的相关对应操作？
//	@ZValue(name = "async.threadNamePrefix", listenForChanges = true)
	private String threadNamePrefix = "zasync-Thread-";

}
