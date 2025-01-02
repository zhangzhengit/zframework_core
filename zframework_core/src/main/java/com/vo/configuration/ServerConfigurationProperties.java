package com.vo.configuration;

import java.util.Map;

import com.vo.anno.ZConfigurationProperties;
import com.vo.anno.ZOrder;
import com.vo.anno.ZValue;
import com.vo.core.PortChecker;
import com.vo.core.QPSEnum;
import com.vo.enums.ZSessionStorageTypeEnum;
import com.vo.validator.ZClientQPSValidator;
import com.vo.validator.ZCustom;
import com.vo.validator.ZMax;
import com.vo.validator.ZMin;
import com.vo.validator.ZNotEmtpy;
import com.vo.validator.ZNotNull;
import com.vo.validator.ZServerQPSValidator;
import com.vo.validator.ZSessionIdQPSValidator;
import com.vo.validator.ZStartWith;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * server相关的配置
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZConfigurationProperties(prefix = "server")
@ZOrder(value = Integer.MIN_VALUE)
public class ServerConfigurationProperties {

	/**
	 * 启动的端口号
	 */
	@ZNotNull
	@ZMin(min = PortChecker.PORT_MIN)
	@ZMax(max = PortChecker.PORT_MAX)
	private Integer port = 80;

	/**
	 * response 响应头中是否包含 Cookie (ZSESSIONID)
	 */
	@ZNotNull
	@ZValue(name = "server.response.z.session.id", listenForChanges = true)
	private Boolean responseZSessionId = true;

	/**
	 * server的name，用于响应头中的Server字段
	 */
	@ZNotNull
	private String name = "ZServer";

	/**
	 * 读取http请求的header的ByteBuffer的容量大小
	 * 并且只有在 GET/TRACE/HEAD 的METHOD时才使用此值
	 * 其他的POST/PUT/PATCH等固定使用1的容量
	 */
	@ZNotNull
	@ZMin(min = 100)
	@ZMax(max = 10240)
	private Integer byteBufferSize = 1024 * 2;

	/**
	 * nio 读取http请求的body时
	 * socketChannel.read()方法一直返回0时的等待超时毫秒数
	 *
	 */
	@ZMin(min = 1)
	@ZMax(max = 1000 * 60)
	@ZNotNull
	private Integer nioReadTimeout = 100 * 5;

	/**
	 * 限制上传文件的最大KB数
	 * 单位：KB
	 */
	@ZMin(min = 1)
	@ZMax(max = 1024 * 1000)
	@ZNotNull
	private Integer uploadFileSize = 1024 * 50;

	/**
	 * 上传文件时从[一次性读取内存]改为[边读边写入到临时文件]的阈值
	 *
	 * 现在为一次性读取到内存的最大值，max值设得小一点防止占用太大内存
	 *
	 * 单位：KB
	 */
	@ZMin(min = 100)
	@ZMax(max = 1024 * 100)
	@ZNotNull
	private Integer uploadFileToTempSize = 1024;

	/**
	 * 上传文件时存放临时文件的目录
	 */
	private String uploadTempDir;

	/**
	 * 处理http请求的最大线程数量
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = 2000)
	private Integer threadCount = Runtime.getRuntime().availableProcessors() * 2;

	/**
	 * 处理http请求的线程的名称前缀，生成的线程以此为前缀分别命名为1、2、3以此类推
	 */
	@ZNotEmtpy
	private String threadName = "http-Thread-";

	/**
	 * 是否启用静态资源的缓存
	 */
	@ZNotNull
	@ZValue(name = "server.static.resource.cache.enable", listenForChanges = true)
	private Boolean staticResourceCacheEnable = true;

	/**
	 * 扫描的包配置，如：com.vo
	 */
	@ZNotNull
	private String scanPackage = "com.vo";

	/**
	 * 是否启用QPS限制 (server.qps)
	 */
	@ZNotNull
	@ZValue(name = "server.qps.limit.enabled", listenForChanges = true)
	private Boolean qpsLimitEnabled = true;

	/**
	 * 接受并且处理http请求的QPS最大值，超过此值会返回非200的status
	 * 此值可配置为0，作为一个feature来让其拒绝一切请求
	 */
	@ZNotNull
	@ZMin(min = 0)
	@ZMax(max = ZServerQPSValidator.MAX_VALUE)
	@ZValue(name = "server.qps", listenForChanges = true)
	@ZCustom(cls = ZServerQPSValidator.class)
	// FIXME 2023年11月15日 下午3:02:12 zhanghen: TODO 是否限制同一个clientip短时间内高频率访问（脚本刷）？
	// 如果不限制的话，是否其他ip的请求优先处理？

	// FIXME 2024年2月15日 下午6:14:59 zhanghen: 最先判断是否超过server.qps 似乎不合理，应该先判断ZSESSIONID或者client是否超对应的qps，然后判断是否超server.qps
	private Integer qps = QPSEnum.SERVER.getDefaultValue();

	/**
	 * 访问超过 本类 [server.qps] 配置值限制时给客户端的提示语
	 */
	@ZNotEmtpy
	@ZValue(name = "server.qps.exceed.message", listenForChanges = true)
	private String qpsExceedMessage = "访问频繁,请稍后再试";

	/**
	 * 当前待处理的请求数最大值限制，来新请求时如果当前待处理请求数已经达到此值，则拒绝本次请求并返回错误码
	 */
	@ZMin(min = 1)
	@ZMax(max = ZServerQPSValidator.MAX_VALUE)
	// FIXME 2024年1月30日 下午7:28:32 zhanghen: 此值按现在的代码逻辑不好实现自动更新，
	// 因为 queue 是程序启动时就初始化了的，改变此值时，若直接set一个新的容量的queue，则有可能queue中有带处理的
	// 考虑是否这么做？还是不用自动更新
	//	@ZValue(name = "server.pending.tasks", listenForChanges = true)
	// XXX 考虑好默认为什么比较好
	private Integer pendingTasks = Runtime.getRuntime().availableProcessors();

	/**
	 *	请求超过 [server.pending.tasks] 配置值时给客户端的提示信息
	 */
	@ZNotEmtpy
	@ZValue(name = "server.pending.tasks.exceed.message", listenForChanges = true)
	private String pendingTasksExceedMessage = "待处理请求队列已满，请稍后再试";

	/**
	 * 对于请求的响应模式
	 */
	@ZCustom(cls = TaskResponsiveModeValidator.class)
	@ZNotEmtpy
	private String taskResponsiveMode = TaskResponsiveModeEnum.QUEUE.name();
	// FIXME 2024年2月10日 下午11:55:37 zhanghen: 新增的两个taskXX readmetxt添加上

	/**
	 * 从服务器接收到请求的时间点开始，到处理本次请求的时间点截止，超过此值就返回【服务器忙】的信息。单位：毫秒
	 *
	 * 注意：仅 taskResponsiveMode=IMMEDIATELY 时，本配置项才生效
	 *
	 */
	@ZMin(min = 1)
	@ZMax(max = 1000 * 100)
	@ZValue(name = "server.task.timeout.milliseconds", listenForChanges = true)
	private Integer taskTimeoutMilliseconds = 100;

	/**
	 * 是否启用对一个client的qps限制
	 */
	@ZNotNull
	@ZValue(name = "server.enable.client.qps", listenForChanges = true)
	private Boolean enableClientQps = true;

	/**
	 * 对一个client的qps限制
	 */
	@ZMin(min = ZClientQPSValidator.MIN_VALUE)
	@ZMax(max = ZClientQPSValidator.MAX_VALUE)
	@ZValue(name = "server.client.qps", listenForChanges = true)
	@ZCustom(cls = ZClientQPSValidator.class)
	private Integer clientQps = QPSEnum.CLIENT.getDefaultValue();

	/**
	 * 对一个ZSESSIONID的qps限制
	 */
	@ZMin(min = ZSessionIdQPSValidator.MIN_VALUE)
	@ZMax(max = ZSessionIdQPSValidator.MAX_VALUE)
	@ZValue(name = "server.session.id.qps", listenForChanges = true)
	@ZCustom(cls = ZSessionIdQPSValidator.class)
	private Integer sessionIdQps = QPSEnum.Z_SESSION_ID.getDefaultValue();

	/**
	 * 是否启用内置的 StaticController,
	 * 注意：如果设为false不启用，则需要手动添加Controller处理 StaticController 类里的
	 * 静态资源
	 */
	@ZNotNull
	private Boolean staticControllerEnable = true;

	/**
	 * 长连接超时时间，一个长连接超过此时间则关闭，单位：秒
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = 86400)
	// FIXME 2023年7月4日 下午6:57:06 zhanghen: TODO 改为：从连接最后一次活动开始计时，超过此值再关闭
	private Integer keepAliveTimeout = 60 * 10;

	/**
	 * session 存储类型
	 */
	@ZNotEmtpy
	@ZCustom(cls = ZSessionStorageTypeValidator.class)
	private String sessionStorageType = ZSessionStorageTypeEnum.MEMORY.name();

	/**
	 * session超时秒数，超时此值则销毁session
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = Integer.MAX_VALUE)
	@ZValue(name = "server.session.timeout", listenForChanges = true)
	private Long sessionTimeout = 60 * 30L;

	/**
	 * 配置硬盘上的资源目录，如：E:\\x
	 * 此值配置了，则优先读取此值下的资源文件
	 * 此值没配置，则读取 staticPrefix 目录下的资源文件
	 */
	private String staticPath;

	/**
	 * 配置读取程序内resources下的资源,
	 * 相对于 resources 目录静态资源的目录，
	 * 如： 配置为 /static，则读取目录为 resources/static
	 */
	@ZNotNull
	@ZStartWith(prefix = "/")
	private String staticPrefix = "/static";

	/**
	 * 是否开启对响应body的压缩
	 */
	@ZNotNull
	private Boolean compressionEnable = true;

	/**
	 * 开启压缩的content-type,如需配置多个，则用,隔开，如： text/html,text/css
	 */
	@ZNotNull
	private String compressionTypes = "text/html,text/css,application/json,application/javascript,text/plain";

	/**
	 * body大于多少KB才启用压缩
	 */
	@ZNotNull
	@ZMin(min = 1)
	private Integer compressionMinLength = 1;

	/**
	 * 支持的自定义响应头header，如：解决CORS问题，配置如下：
	 * server.responseHeaders.Access-Control-Allow-Origin=*
	 */
	private Map<String, String> responseHeaders;

	/**
	 * 程序启动时是否打印 @ZConfigurationProperties 配置类信息
	 */
	@ZNotNull
	private Boolean printConfigurationProperties = false;

	/**
	 * 是否输出生成的代理类源码
	 */
	@ZNotNull
	private Boolean printProxyClass = false;

	public boolean compressionContains(final String contentType) {
		final String[] a = this.getCompressionType();
		for (final String string : a) {
			if (string.equals(contentType)) {
				return true;
			}
		}

		return false;
	}

	public String[] getCompressionType() {
		return SCU.split(this.compressionTypes, ",");
	}

}