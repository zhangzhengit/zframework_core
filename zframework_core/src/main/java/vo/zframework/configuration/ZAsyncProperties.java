package vo.zframework.configuration;

import vo.zframework.anno.ZConfigurationProperties;
import vo.zframework.validator.ZMax;
import vo.zframework.validator.ZMin;
import vo.zframework.validator.ZNotEmtpy;
import vo.zframework.validator.ZNotNull;

/**
 * @ZAsync 用到的线程池的相关配置
 *
 * @author zhangzhen
 * @date 2023年7月8日
 *
 */
@ZConfigurationProperties(prefix = "async")
public class ZAsyncProperties {

	/**
	 * 默认 最低的线程数量
	 */
	private static final int MIN = 10;

	/**
	 * 最大线程数量
	 */
	@ZNotNull
	@ZMin(min = 2)
	@ZMax(max = 100)
	private int threadCount = Math.min(MIN, Runtime.getRuntime().availableProcessors());

	/**
	 * 线程名称前缀
	 */
	@ZNotNull
	@ZNotEmtpy
	private String threadNamePrefix = "async-Thread-";

	public int getThreadCount() {
		return this.threadCount;
	}

	public void setThreadCount(final int threadCount) {
		this.threadCount = threadCount;
	}

	public String getThreadNamePrefix() {
		return this.threadNamePrefix;
	}

	public void setThreadNamePrefix(final String threadNamePrefix) {
		this.threadNamePrefix = threadNamePrefix;
	}

	public static int getMin() {
		return MIN;
	}
	
}
