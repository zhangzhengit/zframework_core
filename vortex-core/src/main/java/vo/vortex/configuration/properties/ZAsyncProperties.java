package vo.vortex.configuration.properties;

import vo.vortex.anno.ZMax;
import vo.vortex.anno.ZMin;
import vo.vortex.anno.ZNotEmtpy;
import vo.vortex.anno.ZOrder;

/**
 *
 * 异步 配置项
 *
 * @author zhangzhen
 * @date 2026年8月9日 17:40:17
 */
@ZConfigurationProperties(prefix = "async")
@ZOrder(value = Integer.MIN_VALUE)
public class ZAsyncProperties {

	/**
	 * 处理事件请求的最大线程数量
	 */
	@ZMin(min = 1)
	@ZMax(max = 1000)
	private final Integer threadCount = Math.min(10, Runtime.getRuntime().availableProcessors());

	/**
	 * 线程名称前缀
	 */
	@ZNotEmtpy
	private final String threadName = "async-Thread-";

	public Integer getThreadCount() {
		return this.threadCount;
	}

	public String getThreadName() {
		return this.threadName;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ZAsyncProperties [threadCount=");
		builder.append(this.threadCount);
		builder.append(", threadName=");
		builder.append(this.threadName);
		builder.append("]");
		return builder.toString();
	}

}
