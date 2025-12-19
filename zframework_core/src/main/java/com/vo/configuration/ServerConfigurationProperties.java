package com.vo.configuration;

import java.util.Map;
import java.util.Set;

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

/**
 * server相关的配置
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
@ZConfigurationProperties(prefix = "server")
@ZOrder(value = Integer.MIN_VALUE)
public class ServerConfigurationProperties {

	static String[] compressionTypeBA = null;

	/**
	 * 启动的端口号
	 */
	@ZNotNull
	@ZMin(min = PortChecker.PORT_MIN)
	@ZMax(max = PortChecker.PORT_MAX)
	private int port = 80;

	/**
	 * response 响应头中是否包含 Cookie (ZSESSIONID)
	 */
	@ZNotNull
	@ZValue(name = "server.response.z.session.id", listenForChanges = true)
	private boolean responseZSessionId = true;

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
	private int byteBufferSize = 1024 * 2;

	/**
	 * nio 读取http请求的body时
	 * socketChannel.read()方法一直返回0时的等待超时毫秒数
	 *
	 */
	@ZMin(min = 1)
	@ZMax(max = 1000 * 60)
	@ZNotNull
	private int nioReadTimeout = 100 * 5;

	/**
	 * 限制上传文件的最大KB数
	 * 单位：KB
	 */
	@ZMin(min = 1)
	@ZMax(max = 1024 * 10000)
	@ZNotNull
	private int uploadFileSize = 1024 * 50;

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
	private int uploadFileToTempSize = 1024;

	/**
	 * 上传文件时存放临时文件的目录，
	 * 默认为[user.dir]下的temp目录
	 *
	 */
	private String uploadTempDir;

	/**
	 * 处理http请求的最大线程数量
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = 2000)
	private int threadCount = Runtime.getRuntime().availableProcessors() * 4;

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
	private boolean staticResourceCacheEnable = true;

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
	private boolean qpsLimitEnabled = true;

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
	private int qps = QPSEnum.SERVER.getDefaultValue();

	/**
	 * 访问超过 本类 [server.qps] 配置值限制时给客户端的提示语
	 */
	@ZNotEmtpy
	@ZValue(name = "server.qps.exceed.message", listenForChanges = true)
	private String qpsExceedMessage = "访问频繁,请稍后再试";

	/**
	 * 请求信息的header的大小限制，单位：字节
	 * 任意一个header的value超过此值，会响应431
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = 1024 * 16)
	private int requestHeaderSizeLimit = 500;

	/**
	 * 当前待处理的请求数最大值限制，来新请求时如果当前待处理请求数已经达到此值，则拒绝本次请求并返回错误码
	 */
	@ZMin(min = 52)
	@ZMax(max = 10000 * 1)
	private int pendingTasks = 100;

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

	/**
	 * 从服务器接收到请求的时间点开始，到处理本次请求的时间点截止，超过此值就返回【服务器忙】的信息。单位：毫秒
	 *
	 * 注意：仅 taskResponsiveMode=IMMEDIATELY 时，本配置项才生效
	 *
	 */
	@ZMin(min = 1)
	@ZMax(max = 1000 * 100)
	@ZValue(name = "server.task.timeout.milliseconds", listenForChanges = true)
	private int taskTimeoutMilliseconds = 100;

	/**
	 * 是否启用对一个client的qps限制
	 */
	@ZNotNull
	@ZValue(name = "server.enable.client.qps", listenForChanges = true)
	private boolean enableClientQps = true;

	/**
	 * 对一个client的qps限制
	 */
	@ZMin(min = ZClientQPSValidator.MIN_VALUE)
	@ZMax(max = ZClientQPSValidator.MAX_VALUE)
	@ZValue(name = "server.client.qps", listenForChanges = true)
	@ZCustom(cls = ZClientQPSValidator.class)
	private int clientQps = QPSEnum.CLIENT.getDefaultValue();

	/**
	 * 对一个ZSESSIONID的qps限制
	 */
	@ZMin(min = ZSessionIdQPSValidator.MIN_VALUE)
	@ZMax(max = ZSessionIdQPSValidator.MAX_VALUE)
	@ZValue(name = "server.session.id.qps", listenForChanges = true)
	@ZCustom(cls = ZSessionIdQPSValidator.class)
	private int sessionIdQps = QPSEnum.Z_SESSION_ID.getDefaultValue();

	/**
	 * 是否启用内置的 StaticController,
	 * 注意：如果设为false不启用，则需要手动添加Controller处理 StaticController 类里的
	 * 静态资源
	 */
	@ZNotNull
	private boolean staticControllerEnable = true;

	/**
	 * StaticController 中允许的Referer，
	 * 如：http://xxx.com/
	 * 非来自此Referer的请求会被拒绝
	 *
	 * 注意：本配置项仅在[staticPath]未配置的情况下，
	 * 		即未启用静态文件服务器的情况下才生效。
	 * 		因为未启用静态文件服务器时，StaticController
	 * 		只处理一些比如html页面上发起的css js image等请求，
	 * 		此时，这些css js image等文件就不是作为静态资源提供出去的，
	 * 		所以非本配置项来源的请求直接拒绝
	 */
	private Set<String> staticControllerReferersAllowed;

	/**
	 * StaticController 放静态文件的内存缓存的最大容量，单位：字节
	 */
	@ZMin(min = 1024 * 1024 * 1)
	@ZMax(max = Integer.MAX_VALUE)
	private int staticControllerMemoryCacheCapacity = 1024 * 1024 * 100;
	
	/**
	 * StaticController 响应时每次读取的BufferSize，单位：字节
	 */
	@ZNotNull
	@ZMin(min = 1024)
	@ZMax(max = 1024 * 1024 * 4)
	private int staticResponseBufferSize = 1024 * 512;

	/**
	 * 长连接超时时间，一个长连接超过此时间则关闭，单位：秒
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = 86400)
	// FIXME 2023年7月4日 下午6:57:06 zhanghen: TODO 改为：从连接最后一次活动开始计时，超过此值再关闭
	private int keepAliveTimeout = 60 * 10;

	/**
	 * session 存储类型
	 */
	@ZNotEmtpy
	@ZCustom(cls = ZSessionStorageTypeValidator.class)
	private String sessionStorageType = ZSessionStorageTypeEnum.MEMORY.name();

	/**
	 * session超时秒数，超时此值则销毁session，
	 * 注意：是指用户session的最大存活时间
	 * 此值不能大于 sessionMaxTimeout
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = 60 * 60 * 24 * 7)
	@ZValue(name = "server.session.timeout", listenForChanges = true)
	private int sessionTimeout = 60 * 30;
	
	// FIXME 2025年12月20日 06:42:51 zhangzhen :  写一个校验器，检验  sessionTimeout 不能大于 sessionMaxTimeout
	// 提示修改其一
	
	/**
	 * session超时时间[秒]最大值限制，用于限制[server.session.timeout]的大小，
	 * 同时设定存储器的超时时间，如果不限制可能导致一直占用内存最终OOM
	 */
	@ZMin(min = 60 * 60)
	@ZMax(max = 60 * 60 * 24 * 10)
	private int sessionMaxTimeout = 60 * 60 * 24 * 10;

	/**
	 * 允许同时存在的session的最大数量，超过此值会自动淘汰最近最少访问的
	 */
	@ZMin(min = 1)
	@ZMax(max = Integer.MAX_VALUE)
	private int sessionMaxActive = 10000 * 100;

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
	private boolean compressionEnable = true;

	/**
	 * 开启压缩的content-type,如需配置多个，则用,隔开，如： text/html,text/css
	 */
	@ZNotNull
	private String compressionTypes =
	"text/html,"
			+ "text/xml,"
			+ "text/csv,"
			+ "text/css,"
			+ "text/plain,"
			+ "text/javascript,"
			+ "application/json,"
			+ "application/xml,"
			+ "application/javascript,"
			+ "image/svg+xml,"
			+ "font/woff2,"
			+ "font/woff,"
			+ "font/otf,"
			+ "font/ttf"
			;

	/**
	 * body大于多少KB才启用压缩
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = 1000)
	private int compressionMinLength = 1;

	/**
	 * 支持的自定义响应头header，如：解决CORS问题，配置如下：
	 * server.responseHeaders.Access-Control-Allow-Origin=*
	 */
	private Map<String, String> responseHeaders;

	/**
	 * 程序启动时是否打印 @ZConfigurationProperties 配置类信息
	 */
	@ZNotNull
	private boolean printConfigurationProperties = false;

	/**
	 * 是否输出生成的代理类源码
	 */
	@ZNotNull
	private boolean printProxyClass = false;
	
	/**
	 *	启动时是否打印banner
	 */
	private boolean showBanner = false;

	/**
	 * 是否打印请求的header
	 */
	private boolean showHttpHeader = false;
	
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
		if (compressionTypeBA == null) {
			compressionTypeBA = this.compressionTypes.split(",");
		}
		return compressionTypeBA;
	}

	public void setShowHttpHeader(final boolean showHttpHeader) {
		this.showHttpHeader = showHttpHeader;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(final int port) {
		this.port = port;
	}

	public boolean getResponseZSessionId() {
		return this.responseZSessionId;
	}

	public void setResponseZSessionId(final boolean responseZSessionId) {
		this.responseZSessionId = responseZSessionId;
	}

	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public int getByteBufferSize() {
		return this.byteBufferSize;
	}

	public void setByteBufferSize(final int byteBufferSize) {
		this.byteBufferSize = byteBufferSize;
	}

	public int getNioReadTimeout() {
		return this.nioReadTimeout;
	}

	public void setNioReadTimeout(final int nioReadTimeout) {
		this.nioReadTimeout = nioReadTimeout;
	}

	public int getUploadFileSize() {
		return this.uploadFileSize;
	}

	public void setUploadFileSize(final int uploadFileSize) {
		this.uploadFileSize = uploadFileSize;
	}

	public int getUploadFileToTempSize() {
		return this.uploadFileToTempSize;
	}

	public void setUploadFileToTempSize(final int uploadFileToTempSize) {
		this.uploadFileToTempSize = uploadFileToTempSize;
	}

	public String getUploadTempDir() {
		return this.uploadTempDir;
	}

	public void setUploadTempDir(final String uploadTempDir) {
		this.uploadTempDir = uploadTempDir;
	}

	public int getThreadCount() {
		return this.threadCount;
	}

	public void setThreadCount(final int threadCount) {
		this.threadCount = threadCount;
	}

	public String getThreadName() {
		return this.threadName;
	}

	public void setThreadName(final String threadName) {
		this.threadName = threadName;
	}

	public boolean getStaticResourceCacheEnable() {
		return this.staticResourceCacheEnable;
	}

	public void setStaticResourceCacheEnable(final boolean staticResourceCacheEnable) {
		this.staticResourceCacheEnable = staticResourceCacheEnable;
	}

	public String getScanPackage() {
		return this.scanPackage;
	}

	public void setScanPackage(final String scanPackage) {
		this.scanPackage = scanPackage;
	}

	public boolean getQpsLimitEnabled() {
		return this.qpsLimitEnabled;
	}

	public void setQpsLimitEnabled(final boolean qpsLimitEnabled) {
		this.qpsLimitEnabled = qpsLimitEnabled;
	}

	public int getQps() {
		return this.qps;
	}

	public void setQps(final int qps) {
		this.qps = qps;
	}

	public String getQpsExceedMessage() {
		return this.qpsExceedMessage;
	}

	public void setQpsExceedMessage(final String qpsExceedMessage) {
		this.qpsExceedMessage = qpsExceedMessage;
	}

	public int getRequestHeaderSizeLimit() {
		return this.requestHeaderSizeLimit;
	}

	public void setRequestHeaderSizeLimit(final int requestHeaderSizeLimit) {
		this.requestHeaderSizeLimit = requestHeaderSizeLimit;
	}

	public int getPendingTasks() {
		return this.pendingTasks;
	}

	public void setPendingTasks(final int pendingTasks) {
		this.pendingTasks = pendingTasks;
	}

	public String getPendingTasksExceedMessage() {
		return this.pendingTasksExceedMessage;
	}

	public void setPendingTasksExceedMessage(final String pendingTasksExceedMessage) {
		this.pendingTasksExceedMessage = pendingTasksExceedMessage;
	}

	public String getTaskResponsiveMode() {
		return this.taskResponsiveMode;
	}

	public void setTaskResponsiveMode(final String taskResponsiveMode) {
		this.taskResponsiveMode = taskResponsiveMode;
	}

	public int getTaskTimeoutMilliseconds() {
		return this.taskTimeoutMilliseconds;
	}

	public void setTaskTimeoutMilliseconds(final int taskTimeoutMilliseconds) {
		this.taskTimeoutMilliseconds = taskTimeoutMilliseconds;
	}

	public boolean getEnableClientQps() {
		return this.enableClientQps;
	}

	public void setEnableClientQps(final boolean enableClientQps) {
		this.enableClientQps = enableClientQps;
	}

	public int getClientQps() {
		return this.clientQps;
	}

	public void setClientQps(final int clientQps) {
		this.clientQps = clientQps;
	}

	public int getSessionIdQps() {
		return this.sessionIdQps;
	}

	public void setSessionIdQps(final int sessionIdQps) {
		this.sessionIdQps = sessionIdQps;
	}

	public boolean getStaticControllerEnable() {
		return this.staticControllerEnable;
	}

	public void setStaticControllerEnable(final boolean staticControllerEnable) {
		this.staticControllerEnable = staticControllerEnable;
	}

	public Set<String> getStaticControllerReferersAllowed() {
		return this.staticControllerReferersAllowed;
	}

	public void setStaticControllerReferersAllowed(final Set<String> staticControllerReferersAllowed) {
		this.staticControllerReferersAllowed = staticControllerReferersAllowed;
	}

	public int getStaticControllerMemoryCacheCapacity() {
		return this.staticControllerMemoryCacheCapacity;
	}

	public void setStaticControllerMemoryCacheCapacity(final int staticControllerMemoryCacheCapacity) {
		this.staticControllerMemoryCacheCapacity = staticControllerMemoryCacheCapacity;
	}

	public int getKeepAliveTimeout() {
		return this.keepAliveTimeout;
	}

	public void setKeepAliveTimeout(final int keepAliveTimeout) {
		this.keepAliveTimeout = keepAliveTimeout;
	}

	public String getSessionStorageType() {
		return this.sessionStorageType;
	}

	public void setSessionStorageType(final String sessionStorageType) {
		this.sessionStorageType = sessionStorageType;
	}

	public int getSessionTimeout() {
		return this.sessionTimeout;
	}

	public void setSessionTimeout(final int sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

	public String getStaticPath() {
		return this.staticPath;
	}

	public void setStaticPath(final String staticPath) {
		this.staticPath = staticPath;
	}

	public String getStaticPrefix() {
		return this.staticPrefix;
	}

	public void setStaticPrefix(final String staticPrefix) {
		this.staticPrefix = staticPrefix;
	}

	public boolean getCompressionEnable() {
		return this.compressionEnable;
	}

	public void setCompressionEnable(final boolean compressionEnable) {
		this.compressionEnable = compressionEnable;
	}

	public String getCompressionTypes() {
		return this.compressionTypes;
	}

	public void setCompressionTypes(final String compressionTypes) {
		this.compressionTypes = compressionTypes;
	}

	public int getCompressionMinLength() {
		return this.compressionMinLength;
	}

	public void setCompressionMinLength(final int compressionMinLength) {
		this.compressionMinLength = compressionMinLength;
	}

	public Map<String, String> getResponseHeaders() {
		return this.responseHeaders;
	}

	public void setResponseHeaders(final Map<String, String> responseHeaders) {
		this.responseHeaders = responseHeaders;
	}

	public boolean getPrintConfigurationProperties() {
		return this.printConfigurationProperties;
	}

	public void setPrintConfigurationProperties(final boolean printConfigurationProperties) {
		this.printConfigurationProperties = printConfigurationProperties;
	}

	public boolean getPrintProxyClass() {
		return this.printProxyClass;
	}

	public void setPrintProxyClass(final boolean printProxyClass) {
		this.printProxyClass = printProxyClass;
	}

	public boolean getShowBanner() {
		return this.showBanner;
	}

	public void setShowBanner(final boolean showBanner) {
		this.showBanner = showBanner;
	}

	public int getStaticResponseBufferSize() {
		return this.staticResponseBufferSize;
	}

	public void setStaticResponseBufferSize(final int staticResponseBufferSize) {
		this.staticResponseBufferSize = staticResponseBufferSize;
	}
	
	public boolean getShowHttpHeader() {
		return this.showHttpHeader;
	}

	public int getSessionMaxActive() {
		return this.sessionMaxActive;
	}

	public void setSessionMaxActive(final int sessionMaxActive) {
		this.sessionMaxActive = sessionMaxActive;
	}

	public long getSessionMaxTimeout() {
		return this.sessionMaxTimeout;
	}

	public void setSessionMaxTimeout(final int sessionMaxTimeout) {
		this.sessionMaxTimeout = sessionMaxTimeout;
	}

	@Override
	public String toString() {
		return "ServerConfigurationProperties [port=" + this.port + ", responseZSessionId=" + this.responseZSessionId + ", name="
				+ this.name + ", byteBufferSize=" + this.byteBufferSize + ", nioReadTimeout=" + this.nioReadTimeout
				+ ", uploadFileSize=" + this.uploadFileSize + ", uploadFileToTempSize=" + this.uploadFileToTempSize
				+ ", uploadTempDir=" + this.uploadTempDir + ", threadCount=" + this.threadCount + ", threadName=" + this.threadName
				+ ", staticResourceCacheEnable=" + this.staticResourceCacheEnable + ", scanPackage=" + this.scanPackage
				+ ", qpsLimitEnabled=" + this.qpsLimitEnabled + ", qps=" + this.qps + ", qpsExceedMessage=" + this.qpsExceedMessage
				+ ", requestHeaderSizeLimit=" + this.requestHeaderSizeLimit + ", pendingTasks=" + this.pendingTasks
				+ ", pendingTasksExceedMessage=" + this.pendingTasksExceedMessage + ", taskResponsiveMode="
				+ this.taskResponsiveMode + ", taskTimeoutMilliseconds=" + this.taskTimeoutMilliseconds + ", enableClientQps="
				+ this.enableClientQps + ", clientQps=" + this.clientQps + ", sessionIdQps=" + this.sessionIdQps
				+ ", staticControllerEnable=" + this.staticControllerEnable + ", staticControllerReferersAllowed="
				+ this.staticControllerReferersAllowed + ", staticControllerMemoryCacheCapacity="
				+ this.staticControllerMemoryCacheCapacity + ", staticResponseBufferSize=" + this.staticResponseBufferSize
				+ ", keepAliveTimeout=" + this.keepAliveTimeout + ", sessionStorageType=" + this.sessionStorageType
				+ ", sessionTimeout=" + this.sessionTimeout + ", sessionMaxTimeout=" + this.sessionMaxTimeout
				+ ", sessionMaxActive=" + this.sessionMaxActive + ", staticPath=" + this.staticPath + ", staticPrefix="
				+ this.staticPrefix + ", compressionEnable=" + this.compressionEnable + ", compressionTypes=" + this.compressionTypes
				+ ", compressionMinLength=" + this.compressionMinLength + ", responseHeaders=" + this.responseHeaders
				+ ", printConfigurationProperties=" + this.printConfigurationProperties + ", printProxyClass="
				+ this.printProxyClass + ", showBanner=" + this.showBanner + ", showHttpHeader=" + this.showHttpHeader + "]";
	}


}