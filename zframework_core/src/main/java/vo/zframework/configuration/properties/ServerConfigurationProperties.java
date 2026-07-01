package vo.zframework.configuration.properties;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import vo.log.common.CU;
import vo.zframework.anno.ZCustom;
import vo.zframework.anno.ZMax;
import vo.zframework.anno.ZMin;
import vo.zframework.anno.ZNotEmtpy;
import vo.zframework.anno.ZNotNull;
import vo.zframework.anno.ZOrder;
import vo.zframework.anno.ZStartWith;
import vo.zframework.anno.ZValue;
import vo.zframework.enums.AcceptEncodingEnum;
import vo.zframework.enums.ContentTypeEnum;
import vo.zframework.enums.MethodEnum;
import vo.zframework.enums.QPSEnum;
import vo.zframework.enums.ZSessionStorageTypeEnum;
import vo.zframework.http.PortChecker;
import vo.zframework.http.ZHeader;
import vo.zframework.validator.StaticResourcePreCompressionAlgorithmValidator;
import vo.zframework.validator.ZClientQPSValidator;
import vo.zframework.validator.ZHttpMethodValidator;
import vo.zframework.validator.ZServerQPSValidator;
import vo.zframework.validator.ZSessionIdQPSValidator;
import vo.zframework.validator.ZSessionStorageTypeValidator;

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
	private boolean responseZSessionId = false;

	/**
	 * server的name，用于响应头中的Server字段
	 */
	@ZNotEmtpy
	private String name = "vo";

	/**
	 * 读取http请求byte[]的容量大小,单位：字节
	 */
	@ZNotNull
	@ZMin(min = 512)
	@ZMax(max = 1024 * 16)
	private int byteBufferSize = 1024 * 4;

	/**
	 * 限制上传文件的最大KB数
	 * 单位：KB
	 */
	@ZMin(min = 1)
	@ZMax(max = 1024 * 1024 * 10)
	@ZNotNull
	// FIXME 2026年5月30日 10:09:10 zhangzhen : 这个改为BIO后，截止现在还没用上，记得用上
	private int uploadFileSize = 1024 * 1024;

	/**
	 * 上传文件时从[一次性读取内存]改为[边读边写入到临时文件]的阈值,单位：KB
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
	 * 处理http请求的线程的名称前缀，生成的线程以此为前缀分别命名为1、2、3以此类推
	 */
	@ZNotEmtpy
	private String threadName = "hT-"; //$NON-NLS-1$

	/**
	 * 是否启用静态资源的缓存
	 */
	@ZNotNull
	@ZValue(name = "server.static.resource.cache.enable", listenForChanges = true)
	private boolean staticResourceCacheEnable = true;

	/**
	 * 是否启用QPS限制 (server.qps)
	 */
	@ZNotNull
	private boolean qpsLimitEnabled = false;

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
	 * header部分的长度超过此值则响应431
	 */
	@ZNotNull
	@ZMin(min = 512)
	@ZMax(max = 1024 * 16)
	private int requestHeaderSizeLimit = 1024 * 4;

	/**
	 * 允许同时存在的连接数
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = 10000 * 100)
	private int connectionLimit = 10000 * 1;

	/**
	 * 是否启用对一个client的qps限制
	 */
	@ZNotNull
	private boolean enableClientQps = false;

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
	 * 支持的METHOD
	 */
	@ZNotEmtpy
	@ZCustom(cls = ZHttpMethodValidator.class)
	private String method = MethodEnum.toVString();

	/**
	 * 是否启用内置的 StaticController,
	 * 注意：如果设为false不启用，则需要手动添加Controller处理 StaticController 类里的
	 * 静态资源
	 */
	@ZNotNull
	private boolean staticControllerEnable = true;

	/**
	 * StaticController 接口响应的Content-Type
	 * <k,v>配置为<文件后缀名,响应的Content-Type>，如:
	 * server.static.controller.content.type.jpg=image/jpg
	 */
	@ZNotEmtpy
	private Map<String, String> staticControllerContentType = initCTM();

	/**
	 * 是否启用静态文件预压缩
	 */
	@ZNotNull
	private boolean staticResourcePreCompressionEnable = true;

	/**
	 * 启用哪些算法对静态文件预压缩
	 */
	@ZNotEmtpy
	@ZCustom(cls = StaticResourcePreCompressionAlgorithmValidator.class)
	private Set<String> staticResourcePreCompressionAlgorithm = Set.of(
			AcceptEncodingEnum.BR.getValue(),
			AcceptEncodingEnum.ZSTD.getValue(),
			AcceptEncodingEnum.GZIP.getValue());

	/**
	 * 对哪些后缀的静态文件进行预压缩
	 */
	@ZNotEmtpy
	private Set<String> staticResourcePreCompressionSuffix = Set.of(
			"html", "htm", "css", "js", "mjs",
			"json", "xml", "svg", "csv", "tsv",
			"txt", "log", "manifest", "ttf", "otf",
			"eot");

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
	 *
	 * 注意：min配置为 compressionMinLength max值，是为了偷懒，不然ZResponde.body(InputStream)
	 * 方法不太好判断是否进行压缩。
	 * 并且min = 64KB 也不算太大
	 *
	 */
	@ZNotNull
	@ZMin(min = 1024 * 64)
	@ZMax(max = 1024 * 1024 * 4)
	private int staticResponseBufferSize = 1024 * 512;

	/**
	 * 长连接超时时间，一个长连接超过此时间则关闭，单位：秒
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = 60 * 2)
	// FIXME 2023年7月4日 下午6:57:06 zhanghen: TODO 改为：从连接最后一次活动开始计时，超过此值再关闭
	// FIXME 2026年6月18日 11:14:00 zhangzhen : 本分支改为虚拟线程后，此字段还没用上，记得用上
	// 新增一个连接类，一个超时任务的线程类
	// 每次请求来了/响应结束了，都更新一下最后活跃时间，很耗时的流下载，也要write时更新
	// 但是比如超时10S，业务方法执行12S，此时就需要给连接类加一个状态机，超时任务
	// 判断是[执行中]则跳过。或者readme中提示用户耗时长的响应204，待会来取？
	private int keepAliveTimeout = 10;

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
	 * session超时时间，单位：秒
	 * 用于限制[server.session.timeout]的大小
	 */
	@ZMin(min = 10)
	@ZMax(max = 60 * 60 * 24 * 10)
	private int sessionMaxTimeout = 60 * 31;

	/**
	 * 允许同时存在的session的最大数量，超过此值会自动删除最近最少访问的
	 */
	@ZMin(min = 1)
	@ZMax(max = Integer.MAX_VALUE)
	private int sessionMaxActive = 10000 * 100;

	/**
	 * 允许内存中同时存在的session的最大数量，超过此值会自动把最近最少访问的存入DB
	 */
	@ZMin(min = 1)
	@ZMax(max = 10000 * 50)
	private int sessionMaxActiveInMemory = 10000 * 10;

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
	 * 在读完了请求后，立即解析的请求头
	 */
	@ZNotEmtpy
	// FIXME 2026年6月18日 04:31:38 zhangzhen : 这个记得用上，现在还没用上
	private String immediatelyParsedRequestHeaders = "Host,Expect,Upgrade,Connection,Content-Length,Transfer-Encoding";

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
	 * body达到多少KB才对响应body进行压缩，单位：KB
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = 64)
	private int compressionMinLength = 1;

	/**
	 * 支持的自定义响应头header，如：解决CORS问题，配置如下：
	 * server.responseHeaders.Access-Control-Allow-Origin=*
	 */
	private Map<String, String> responseHeaders;

	/**
	 * response中用于暂存响应的byte[]的动态数组的默认初始容量，单位：字节
	 * 此值只是默认容量，不影响后续的扩容，但是扩容后实际存储的byte个数>此值时，
	 * 动态数组会重置为此值
	 * 即：
	 * 	此值设置过小，容易导致频繁的扩容和缩容的arraycopy成为内存热点；
	 *	此值设置过大，又会导致连接存活期间动态数组一直浪费内存
	 */
	@ZMin(min = 512)
	@ZMax(max = 1024 * 8)
	private int responseArrayCapacity = 1024 * 4;

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
	 * 是否打印请求的header
	 */
	private boolean showHttpHeader = false;

	public boolean compressionContains(final String contentType) {
		final String[] cta = this.getCompressionType();
		for (final String ct : cta) {
			if (contentType.startsWith(ct)) {
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

	public boolean isResponseZSessionId() {
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

	public int getUploadFileSize() {
		return this.uploadFileSize;
	}

	public void setUploadFileSize(final int uploadFileSize) {
		this.uploadFileSize = uploadFileSize;
	}

	public String getUploadTempDir() {
		return this.uploadTempDir;
	}

	public void setUploadTempDir(final String uploadTempDir) {
		this.uploadTempDir = uploadTempDir;
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

	public String getImmediatelyParsedRequestHeaders() {
		return this.immediatelyParsedRequestHeaders;
	}

	public void setImmediatelyParsedRequestHeaders(final String immediatelyParsedRequestHeaders) {
		this.immediatelyParsedRequestHeaders = immediatelyParsedRequestHeaders;
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

	public ZHeader[] getResponseHeadersBytes() {

		if (CU.isEmpty(this.getResponseHeaders())) {
			return null;
		}

		final Set<Entry<String, String>> es = this.getResponseHeaders().entrySet();
		final ZHeader[] a = new ZHeader[es.size()];
		int c = 0;
		for (final Entry<String, String> e : es) {
			final ZHeader header = new ZHeader(e.getKey().getBytes(), e.getValue().getBytes());
			a[c] = header;
			c++;
		}

		return a;
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

	public int getSessionMaxActiveInMemory() {
		return this.sessionMaxActiveInMemory;
	}

	public void setSessionMaxActiveInMemory(final int sessionMaxActiveInMemory) {
		this.sessionMaxActiveInMemory = sessionMaxActiveInMemory;
	}

	public Map<String, String> getStaticControllerContentType() {
		return this.staticControllerContentType;
	}

	public void setStaticControllerContentType(final Map<String, String> staticControllerContentType) {
		this.staticControllerContentType = staticControllerContentType;
	}


	static Map<String, String> initCTM() {
		final Map<String, String> ctm = new HashMap<>(16, 1F);
		ctm.put("jpg", ContentTypeEnum.IMAGE_JPG.getType());
		ctm.put("jpeg", ContentTypeEnum.IMAGE_JPGE.getType());
		ctm.put("gif", ContentTypeEnum.IMAGE_GIF.getType());
		ctm.put("png", ContentTypeEnum.IMAGE_PNG.getType());
		ctm.put("ico", ContentTypeEnum.IMAGE_ICON.getType());

		ctm.put("wav", ContentTypeEnum.AUDIO_WAV.getType());
		ctm.put("mp3", ContentTypeEnum.AUDIO_MP3.getType());
		ctm.put("mp4", ContentTypeEnum.VIDEO_MP4.getType());
		ctm.put("pdf", ContentTypeEnum.APPLICATION_PDF.getType());

		ctm.put("doc", ContentTypeEnum.WORD.getType());
		ctm.put("js", ContentTypeEnum.JS.getType());

		ctm.put("txt", new String(ContentTypeEnum.TEXT_PLAIN.getTypeBytes()));
		ctm.put("css", new String(ContentTypeEnum.TEXT_CSS.getTypeBytes()));
		ctm.put("html", new String(ContentTypeEnum.TEXT_HTML.getTypeBytes()));


		return ctm;
	}

	public String getMethod() {
		return this.method;
	}

	public void setMethod(final String method) {
		this.method = method;
	}

	public int getUploadFileToTempSize() {
		return this.uploadFileToTempSize;
	}

	public void setUploadFileToTempSize(final int uploadFileToTempSize) {
		this.uploadFileToTempSize = uploadFileToTempSize;
	}

	public int getResponseArrayCapacity() {
		return this.responseArrayCapacity;
	}

	public void setResponseArrayCapacity(final int responseArrayCapacity) {
		this.responseArrayCapacity = responseArrayCapacity;
	}

	public Set<String> getStaticResourcePreCompressionSuffix() {
		return this.staticResourcePreCompressionSuffix;
	}

	public void setStaticResourcePreCompressionSuffix(final Set<String> staticResourcePreCompressionSuffix) {
		this.staticResourcePreCompressionSuffix = staticResourcePreCompressionSuffix;
	}

	public boolean isStaticResourcePreCompressionEnable() {
		return this.staticResourcePreCompressionEnable;
	}

	public void setStaticResourcePreCompressionEnable(final boolean staticResourcePreCompressionEnable) {
		this.staticResourcePreCompressionEnable = staticResourcePreCompressionEnable;
	}

	public Set<String> getStaticResourcePreCompressionAlgorithm() {
		return this.staticResourcePreCompressionAlgorithm;
	}

	public void setStaticResourcePreCompressionAlgorithm(final Set<String> staticResourcePreCompressionAlgorithm) {
		this.staticResourcePreCompressionAlgorithm = staticResourcePreCompressionAlgorithm;
	}

	public int getConnectionLimit() {
		return this.connectionLimit;
	}

	public void setConnectionLimit(final int connectionLimit) {
		this.connectionLimit = connectionLimit;
	}


}