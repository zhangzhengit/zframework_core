package vo.zframework.core;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import vo.zframework.cache.AU;
import vo.zframework.cache.CU;
import vo.zframework.cache.STU;
import vo.zframework.configuration.ServerConfigurationProperties;
import vo.zframework.enums.ConnectionEnum;
import vo.zframework.enums.MethodEnum;
import vo.zframework.exception.ParseHTTPRequestException;
import vo.zframework.http.HttpStatusEnum;
import vo.zframework.http.ZCookie;

/**
 * 表示http 的请求信息
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
// FIXME 2025年12月20日 07:40:29 zhangzhen :  写功能：请求来了，如果带来了ZSESSIONID并且存在
// 则活跃一下，让存活时间重新计算
public class ZRequest {

	public static final String HTTP_11 = "HTTP/1.1";
	public static final String BOUNDARY = "boundary=";
	public static final ServerConfigurationProperties SERVERCONFIGURATIONPROPERTIES = ZContext
			.getBean(ServerConfigurationProperties.class);
	public static final int requestHeaderSizeLimit = SERVERCONFIGURATIONPROPERTIES.getRequestHeaderSizeLimit();
	public static final String MULTIPART_FORM_DATA = "multipart/form-data";

	// -------------------------------------------------------------------------------------------------
	private final List<String> lineList = new ArrayList<>(10);

	/**
	 *	请求行一行完整内容如：GET / HTTP/1.1
	 */
	String original;

	/**
	 * path中?后面的部分
	 */
	String queryString;

	TF tf;

	/**
	 * 请求方法
	 */
	private MethodEnum methodEnum;

	/**
	 * 完整的requestURI，如：/hello?name=z&age=20
	 */
	String requestURI;

	/**
	 * 简单的path，不含参数，如：/hello
	 */
	String path;

	Set<RequestParam> paramSet;

	/**
	 * http版本
	 */
	String version;

	/**
	 * 请求头,如： Accept-Encoding: gzip, deflate
	 */
	private final Map<String, String> headerMap = new HashMap<>();

	/**
	 * http完整的请求信息
	 */
	private byte[] originalRequestBytes;

	/**
	 * http中body部分
	 */
	private byte[] body;

	/**
	 * 客户端IP
	 */
	private String clientIp;


	final Socket socket;

	/**
	 * 对 isKeepAlive方法结果的暂存
	 * -1 未设置过 0 否 1 是
	 */
	private volatile int keepAlive = -1;

	public boolean isSupportZSTD() {
		return this.supportCompression(AcceptEncodingEnum.ZSTD);
	}

	public boolean isSupportDEFLATE() {
		return this.supportCompression(AcceptEncodingEnum.DEFLATE);
	}

	public boolean isSupportGZIP() {
		return this.supportCompression(AcceptEncodingEnum.GZIP);
	}

	private boolean supportCompression(final AcceptEncodingEnum aeEnum) {
		final String a = this.getHeader(HeaderEnum.ACCEPT_ENCODING.getName());
		if (STU.isEmpty(a)) {
			return false;
		}

		final String[] array = a.split(",");
		for (final String a2 : array) {
			if (aeEnum.getValue().equalsIgnoreCase(a2.trim())) {
				return true;
			}
		}

		return false;
	}

	public String getHost() {
		return this.getHeader(HeaderEnum.HOST.getName());
	}

	public String getMethod() {
		return this.getMethodEnum().getMethod();
	}

	public byte[] getBody() {
		return this.body;
	}

	public int getServerPort() {

		final String host = this.getHeaderMap().get(HeaderEnum.HOST.getName());

		final int i = host.indexOf(STU.COLON);
		if (i > -1) {
			final String port = host.substring(i + 1);
			return Integer.parseInt(port);
		}

		return ZServer.DEFAULT_HTTP_PORT;
	}

	public String getRequestURL() {
		return this.getHost() + this.getRequestURI();
	}

	public String getRequestURI() {
		return this.requestURI;
	}

	/**
	 * 获取Content-Type值
	 *
	 * @return
	 */
	public String getContentType() {
		return this.getHeader(HeaderEnum.CONTENT_TYPE.getName());
	}

	/**
	 * 获取Content-Type为multipart/form-data时的boundary值，非multipart/form-data则返回null
	 * 如：
	 * 		Content-Type: multipart/form-data; boundary=----WebKitFormBoundaryk6aoPrFv24xMcfUf
	 * 则本方法返回内容为：
	 * 		----WebKitFormBoundaryk6aoPrFv24xMcfUf
	 *
	 * @return
	 */
	public String getBoundary() {
		if (!this.isContentTypeFormData()) {
			return null;
		}
		final String ct = this.getHeaderMap().get(HeaderEnum.CONTENT_TYPE.getName());

		final int i = ct.indexOf(BOUNDARY);
		if (i > -1) {
			return ct.substring(i + BOUNDARY.length());
		}

		return null;
	}

	/**
	 * 判断Content-Type是否multipart/form-data
	 *
	 * @return
	 */
	public boolean isContentTypeFormData() {
		final String ct = this.getContentType();
		return ct == null ? false : ct.contains(MULTIPART_FORM_DATA);
	}

	/**
	 * 返回指定名称的session，无则返回null
	 *
	 * @param name
	 * @return
	 *
	 */
	public ZSession getSession(final String name) {
		return ZSessionMap.get(name);
	}

	public ZSession getSession() {
		return this.getSession(true);
	}

	/**
	 * 获取Session，如需写入到Cookie，需要自己处理 ZResponse.cookie.write................
	 *
	 * @param create
	 * @return
	 */
	public synchronized ZSession getSession(final boolean create) {
		final ZCookie[] cs = this.getCookies();

		if (AU.isNotEmpty(cs)) {
			for (final ZCookie zc : cs) {
				if (HeaderEnum.Z_SESSION_ID.getName().equals(zc.getName())) {
					final ZSession session = ZSessionMap.get(zc.getValue());

					if (session != null) {
						return session;
					}

					// session == null 可能是服务器重启了等
					if (!create) {
						return null;
					}

					final ZSession newSession = ZRequest.newSession();
					return newSession;
				}
			}
		}

		if (!create) {
			return null;
		}

		final ZSession newSession = ZRequest.newSession();
		return newSession;
	}

	public static ZSession newSession() {
		return new ZSession();
	}

	public int getContentLength() {
		final String s = this.getHeader(HeaderEnum.CONTENT_LENGTH.getName());
		return s == null ? -1 : Integer.parseInt(s);
	}

	public ZCookie getCookie(final String name) {

		final String cookisString = this.getHeaderMap().get(HeaderEnum.COOKIE.getName());
		if (STU.isEmpty(cookisString)) {
			return null;
		}

		final String[] a = cookisString.split(STU.SEMICOLON);
		for (final String s : a) {
			final String[] c1 = s.split(STU.EQUALS);
			if (c1[0].trim().equals(name)) {
				final ZCookie zCookie = new ZCookie(c1[0].trim(), c1[1].trim());
				return zCookie;
			}
		}

		return null;
	}

	public ZCookie[] getCookies() {

		final String cookisString = this.getHeaderMap().get(HeaderEnum.COOKIE.getName());
		if (STU.isEmpty(cookisString)) {
			return null;
		}

		final String[] a = cookisString.split(STU.SEMICOLON);
		final ZCookie[] c = new ZCookie[a.length];
		int cI = 0;
		for (final String s : a) {
			final String[] c1 = s.split(STU.EQUALS);
			final ZCookie zCookie = new ZCookie(c1[0].trim(),c1[1].trim());

			c[cI] = zCookie;
			cI++;
		}

		return c;
	}

	public ZCookie getZSESSIONID() {
		final ZCookie[] cookies = this.getCookies();
		if (AU.isEmpty(cookies)) {
			return null;
		}

		for (final ZCookie zCookie : cookies) {
			if(HeaderEnum.Z_SESSION_ID.getName().equals(zCookie.getName())) {
				return zCookie;
			}
		}

		return null;
	}

	public String getUserAgent() {
		return this.getHeaderMap().get(HeaderEnum.USER_AGENT.getName());
	}

	public String getHeader(final String name) {
		return this.getHeaderMap().get(name);
	}

	public boolean isKeepAlive() {
		if (this.keepAlive != -1) {
			return this.keepAlive == 1;
		}

		final String connection = this.getHeader(HeaderEnum.CONNECTION.getName());
		final boolean keepAlive = STU.isNotEmpty(connection)
				&& (connection.length() == ConnectionEnum.KEEP_ALIVE.getValue().length())
				&& (connection.equals(ConnectionEnum.KEEP_ALIVE.getValue())
						|| connection.toLowerCase().contains(ConnectionEnum.KEEP_ALIVE.getValue().toLowerCase()));

		this.keepAlive = keepAlive ? 1 : 0;

		return keepAlive;
	}

	public Object getParameter(final String name) {
		// FIXME 2024年12月9日 下午6:30:42 zhangzhen : 这个方法是否要改
		// 因为@ZRequestParam加入了默认值，用此方法取还是原值而非默认值
		final Set<RequestParam> ps = this.getParamSet();
		if (CU.isEmpty(ps)) {
			return null;
		}

		for (final RequestParam requestParam : ps) {
			if (requestParam.getName().equals(name)) {
				return requestParam.getValue();
			}
		}

		return null;
	}

	public List<String> getLineList() {
		return this.lineList;
	}

	public ZRequest(final List<String> lineList) {
		this.socket = SocketTL.get();
		this.lineList.addAll(lineList);
		parseRequest(this);
	}

	private static ZRequest parseRequest(final ZRequest request) {
		if (CU.isEmpty(request.getLineList())) {
			return request;
		}

		// 0 为 请求行
		final String line = request.getLineList().get(0);
		request.original = line;

		final int methodIndex = line.indexOf(STU.SAPCE);

		// path
		parsePath(line, request, methodIndex);

		// paserHeader
		parseHeader(request);

		// HTTP1.1必须有 HOST 头
		final String header = request.getHost();
		if (STU.isNullOrEmptyOrBlank(header)) {
			throw new IllegalArgumentException("缺少 " + HeaderEnum.HOST.getName() + " 头");
		}

		// parseBody
		// FIXME 2024年12月9日 下午6:32:57 zhangzhen : 不需要parseBody了，在BodyReader里面已经setBody(byte[])了
		//		parseBody(request, requestLine);

		return request;
	}

	private String getClientIp0() {

		final String xRealIp = this.getHeader(HeaderEnum.X_REAL_IP.getName());
		if (xRealIp != null) {
			return xRealIp;
		}

		final String xForwardedFor = this.getHeader(HeaderEnum.X_Forwarded_For.getName());
		if (xForwardedFor != null) {
			return xForwardedFor;
		}

		// FIXME 2023年11月16日 下午2:47:38 zhanghen: ab 测试这里可能取不到,修复掉
		if (this.socket == null) {
			return null;
		}

		final InetSocketAddress inetSocketAddress = (InetSocketAddress) this.socket.getRemoteSocketAddress();
		if (inetSocketAddress == null) {
			return null;
		}

		final InetAddress address = inetSocketAddress.getAddress();
		return address.getHostAddress();
	}


	private static void parsePath(final String s, final ZRequest request, final int methodIndex) {
		final String requestURI = parseURI(s, request, methodIndex);

		try {
			request.requestURI = java.net.URLDecoder.decode(requestURI, Task.DEFAULT_CHARSET_NAME);
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public static String parsePATH(final String requestLine) {
		final int si = requestLine.indexOf(STU.SAPCE);
		if (si > -1) {
			final int s2i = requestLine.indexOf(STU.SAPCE, si + 1);
			if (s2i > -1) {
				final String uri = requestLine.substring(si + 1, s2i);
				if (STU.isNotEmpty(uri)) {
					final int wenI = uri.indexOf("?");
					if (wenI > -1) {
						final String path = uri.substring(0, wenI);
						return path;
					}
					return uri;
				}
			}
		}

		return null;
	}

	private static String parseURI(final String s, final ZRequest request, final int methodIndex) {
		final int pathI = s.indexOf(STU.SAPCE, methodIndex + 1);
		if (pathI <= -1) {
			throw new IllegalArgumentException("请求行错误：找不到path");
		}

		final String requestURI = s.substring(methodIndex  + 1, pathI);

		final int wenI = requestURI.indexOf("?");
		if (wenI > -1) {
			request.queryString = requestURI.substring(("?".length() + wenI) - 1);
			final Set<RequestParam> paramSet = new HashSet<>();
			final String param = requestURI.substring("?".length() + wenI);
			final String simplePath = requestURI.substring(0,wenI);

			try {
				request.path = java.net.URLDecoder.decode(simplePath, Task.DEFAULT_CHARSET_NAME);
			} catch (final UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			final String[] paramArray = param.split(Task.SP);
			for (final String p : paramArray) {
				final String[] p0 = p.split(STU.EQUALS);
				final ZRequest.RequestParam requestParam = new ZRequest.RequestParam();
				requestParam.setName(p0[0]);
				if (p0.length >= 2) {
					try {
						final String v = STU.isEmpty(p0[1]) ? STU.EMPTY
								: java.net.URLDecoder.decode(p0[1], Task.DEFAULT_CHARSET_NAME);
						requestParam.setValue(v);
					} catch (final UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				} else {
					requestParam.setValue(STU.EMPTY);
				}

				paramSet.add(requestParam);
			}

			request.paramSet = paramSet;

		} else {
			try {
				request.path = java.net.URLDecoder.decode(requestURI, Task.DEFAULT_CHARSET_NAME);
			} catch (final UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return requestURI;
	}

	private static void parseHost(final String line, final ZRequest request) {

	}

	private static void parseVersion(final String requestLine, final ZRequest request) {
		final int hI = requestLine.lastIndexOf("HTTP/");
		if (hI <= -1) {
			throw new IllegalArgumentException("请求行错误：找不到HTTP版本");
		}
		final String version = requestLine.substring(hI);
		if (!HTTP_11.equalsIgnoreCase(version)) {
			// FIXME 2024年12月19日 下午1:41:45 zhangzhen : ab 命令测试会走到异常，要不要抛异常以后再看
			//				throw new IllegalArgumentException("请求行错误：HTTP版本错误,本服务器支持HTTP/1.1");
		}
		request.version = version;
	}

	private static void parseHeader(final ZRequest request) {
		final List<String> x = request.getLineList();
		for (int i = x.size() - 1; i > 0; i--) {
			final String l = x.get(i);
			if (STU.EMPTY.equals(l)) {
				continue;
			}

			final int k = l.indexOf(STU.COLON);
			if ((k + 1 + 1) >= l.length()) {
				final String key = l.substring(0, k).trim();
				request.headerMap.put(key, "");
				continue;
			}

			if (k > -1) {

				final String value = l.substring(k + 1).trim();
				// CONTENT_LENGTH 头在bodyreader.readHeader时已经校验过了，在此肯定非负的整数

				if (STU.hasContent(value)) {

					final int length = value.length();
					// FIXME 2025年1月20日 下午8:45:36 zhangzhen : 要不要用value.getBytes().length 判断？
					// 判断的话会太费性能
					if (length > requestHeaderSizeLimit) {
						throw new ParseHTTPRequestException(HttpStatusEnum.HTTP_431.getMessage(),
								HttpStatusEnum.HTTP_431.getStatus());
					}
				}

				final String key = l.substring(0, k).trim();
				// FIXME 2026年6月7日 05:45:39 zhangzhen : 奇怪了，k和v去掉trim()反而变慢？
				request.headerMap.put(key, value);
			}
		}

	}

	public String getOriginal() {
		return this.original;
	}

	public String getQueryString() {
		return this.queryString;
	}

	public TF getTf() {
		return this.tf;
	}

	public void setTf(final TF tf) {
		this.tf = tf;
	}

	public MethodEnum getMethodEnum() {
		return this.methodEnum;
	}


	public String getPath() {
		return this.path;
	}

	public Set<RequestParam> getParamSet() {
		return this.paramSet;
	}

	public String getVersion() {
		return this.version;
	}

	public Map<String, String> getHeaderMap() {
		return this.headerMap;
	}

	public byte[] getOriginalRequestBytes() {
		return this.originalRequestBytes;
	}

	public void setOriginalRequestBytes(final byte[] originalRequestBytes) {
		this.originalRequestBytes = originalRequestBytes;
	}

	public String getClientIp() {
		if (this.clientIp == null) {
			this.clientIp = this.getClientIp0();
		}
		return this.clientIp;
	}

	public void setBody(final byte[] body) {
		this.body = body;
	}

	public static class RequestParam {

		private String name;
		private Object value;

		public RequestParam(final String name, final Object value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return this.name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public void setValue(final Object value) {
			this.value = value;
		}

		public Object getValue() {
			return this.value;
		}

		public RequestParam() {
		}

	}

	public static class ZHeader {

		private String name;
		private String value;
		public String getName() {
			return this.name;
		}
		public void setName(final String name) {
			this.name = name;
		}
		public String getValue() {
			return this.value;
		}
		public void setValue(final String value) {
			this.value = value;
		}
		public ZHeader(final String name, final String value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public String toString() {
			return "ZHeader [name=" + this.name + ", value=" + this.value + "]";
		}

	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ZRequest [lineList=");
		builder.append(this.lineList);
		builder.append(", original=");
		builder.append(this.original);
		builder.append(", queryString=");
		builder.append(this.queryString);
		builder.append(", tf=");
		builder.append(this.tf);
		builder.append(", methodEnum=");
		builder.append(this.getMethodEnum());
		builder.append(", requestURI=");
		builder.append(this.requestURI);
		builder.append(", path=");
		builder.append(this.path);
		builder.append(", paramSet=");
		builder.append(this.paramSet);
		builder.append(", version=");
		builder.append(this.version);
		builder.append(", headerMap=");
		builder.append(this.headerMap);
		builder.append(", originalRequestBytes=");
		builder.append(Arrays.toString(this.originalRequestBytes));
		builder.append(", body=");
		builder.append(Arrays.toString(this.body));
		builder.append(", clientIp=");
		builder.append(this.clientIp);
		builder.append(", socket=");
		builder.append(this.socket);
		builder.append("]");
		return builder.toString();
	}

	public void setMethodEnum(final MethodEnum methodEnum) {
		this.methodEnum = methodEnum;
	}

}
