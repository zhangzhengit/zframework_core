package vo.zframework.core;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import vo.zframework.cache.AU;
import vo.zframework.cache.ArrayRange;
import vo.zframework.cache.CU;
import vo.zframework.cache.STU;
import vo.zframework.configuration.ServerConfigurationProperties;
import vo.zframework.enums.ConnectionEnum;
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
	public static final byte[] HTTP_11_BYTES = HTTP_11.getBytes();
	public static final String BOUNDARY = "boundary=";
	private static final char SPACE = STU.SPACE_CHAR;
	private static final ZCookie[] EMPTY_ZCOOKIE = {};
	private static final String HEADER_PARSED_NO_VALUE = "\u0000" + "\0" + "PARSED_NO_VALUE" + UUID.randomUUID();
	private static final int HEADER_PARSED_NO_VALUE_LENGTH = HEADER_PARSED_NO_VALUE.length();
	public static final ServerConfigurationProperties SERVERCONFIGURATIONPROPERTIES = ZContext
			.getBean(ServerConfigurationProperties.class);
	public static final int requestHeaderSizeLimit = SERVERCONFIGURATIONPROPERTIES.getRequestHeaderSizeLimit();
	public static final String MULTIPART_FORM_DATA = "multipart/form-data";

	// -------------------------------------------------------------------------------------------------

	/**
	 * 一个http请求的完整byte[]
	 */
	private final byte[] dataRawArray;

	/**
	 * 标记了 dataRawArray 里的每个header的起止位置
	 */
	private final List<ArrayRange> arList;

	/**
	 *	请求行一行完整内容如：GET / HTTP/1.1
	 */
	String original;

	/**
	 * path中?后面的部分
	 */
	BA queryStringBA;
	String queryStringCache;

	TF tf;

	/**
	 * 请求方法 byte[]
	 */
	private byte[] methodNameBytes;

	private
	String methodCache;

	/**
	 * 完整的requestURI，如：/hello?name=z&age=20
	 */
	String requestURI;

	/**
	 * 简单的path，不含参数，如：/hello
	 */
	String path;

	List<RequestParam> params;

	/**
	 * http版本
	 */
	String version;

	/**
	 * 请求头,如： Accept-Encoding: gzip, deflate
	 */
	// FIXME 2026年6月11日 19:52:40 zhangzhen : 解析和匹配时，要不要处理为大小写统一风格？
	private final Map<String, String> headerMap;

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

	/**
	 * 暂存值
	 */
	ZCookie[] cookies;

	/**
	 * 暂存值
	 */
	String userAgent = null;

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
		final String ae = this.getAcceptEncoding();
		if (STU.isEmpty(ae)) {
			return false;
		}

		// FIXME 2026年6月20日 15:53:29 zhangzhen : 下面会生成多个String成为内存热点，
		// 但是似乎只能这样，使用byte[]貌似不好处理，连if ((ae.indexOf(aeEnum.getValue()) > -1)
		//	 || (ae.toLowerCase().indexOf(aeEnum.getValue()) > -1)) 也不行，虽然简单,
		// 但只能匹配正规的合法的且严格小写的AcceptEncoding
		// 还会有误判，如恶意请求的agzipb也会误判为gzip，虽无安全问题，但也多做了无用的压缩
		final String[] array = ae.split(",");
		for (final String a : array) {
			if (aeEnum.getValue().equals(a)
			 ||	aeEnum.getValue().equals(a.trim())
			 ||	aeEnum.getValue().equals(a.toLowerCase())
			 ||	aeEnum.getValue().equals(a.trim().toLowerCase())
					) {
				return true;
			}
		}

		return false;
	}

	public String getHost() {
		return this.getHeader(HeaderEnum.HOST.getName());
	}

	public String getMethod() {
		if (this.methodCache == null) {
			this.methodCache = new String(this.methodNameBytes);
		}

		return this.methodCache;
	}

	public byte[] getBody() {
		return this.body;
	}

	public int getServerPort() {

		final String host = this.getHeader(HeaderEnum.HOST.getName());

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

		final String ct = this.getHeader(HeaderEnum.CONTENT_TYPE.getName());
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

	public long getContentLength() {
		final String s = this.getHeader(HeaderEnum.CONTENT_LENGTH.getName());
		return s == null ? -1 : Long.parseLong(s);
	}

	public ZCookie getCookie(final String name) {
		return this.gcn(name);
	}

	public ZCookie[] getCookies() {

		if (this.cookies == null) {
			this.cookies = this.gcs();
		}

		return this.cookies;
	}

	private ZCookie gcn(final String cookieName) {
		final String cookieString = this.getHeader(HeaderEnum.COOKIE.getName());
		if (STU.isEmpty(cookieString)) {
			return null;
		}

		final int nI = cookieString.indexOf(cookieName);
		if (nI <= -1) {
			return null;
		}

		final int dI = cookieString.indexOf("=", nI + cookieName.length());
		if (dI <= -1) {
			return null;
		}

		final int fI = cookieString.indexOf(";", dI + 1);
		if (fI <= -1) {
			if (cookieString.length() > dI) {
				final String substring = cookieString.substring(nI);
				final String[] c1 = substring.split(STU.EQUALS);
				final ZCookie zCookie = new ZCookie(c1[0].trim(), c1[1].trim());
				return zCookie;
			}
			if (cookieString.length() == dI) {
				return null;
			}
		}

		final String substring = cookieString.substring(nI, fI);
		final String[] c1 = substring.split(STU.EQUALS);
		final ZCookie zCookie = new ZCookie(c1[0].trim(), c1[1].trim());

		return zCookie;
	}

	private ZCookie[] gcs() {
		final String cookisString = this.getHeader(HeaderEnum.COOKIE.getName());
		if (STU.isEmpty(cookisString)) {
			return EMPTY_ZCOOKIE;
		}

		final int si = cookisString.indexOf(STU.SEMICOLON);
		if (si <= -1) {
			final ZCookie[] c = new ZCookie[1];

			final String[] c1 = cookisString.split(STU.EQUALS);
			final ZCookie zCookie = new ZCookie(c1[0].trim(), c1[1].trim());

			c[0] = zCookie;

			return c;
		}

		final String[] a = cookisString.split(STU.SEMICOLON);
		final ZCookie[] c = new ZCookie[a.length];
		int cI = 0;
		for (final String s : a) {
			final String[] c1 = s.split(STU.EQUALS);
			final ZCookie zCookie = new ZCookie(c1[0].trim(), c1[1].trim());

			c[cI] = zCookie;
			cI++;
		}

		return c;
	}

	public ZCookie getZSESSIONID() {
		return this.gcn(HeaderEnum.Z_SESSION_ID.getName());
	}

	public String getUserAgent() {
		if (this.userAgent == null) {
			this.userAgent = this.getHeader(HeaderEnum.USER_AGENT.getName());
		}

		return this.userAgent;
	}

	public String getAcceptEncoding() {
		final String acceptEncoding = this.getHeader(HeaderEnum.ACCEPT_ENCODING.getName());
		return acceptEncoding;
	}

	public String getHeader(final String name) {

		final String v = this.headerMap.get(name);
		if (isHPNV(v)) {
			return null;
		}

		if (v != null) {
			return v;
		}

		final String nV = parseHeaderARHeader(this, name);
		if (isHPNV(nV)) {
			return null;
		}

		return nV;
	}

	private static boolean isHPNV(final String v) {
		return (v != null) && (v.length() == HEADER_PARSED_NO_VALUE_LENGTH) && (v == HEADER_PARSED_NO_VALUE);
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
		final List<RequestParam> ps = this.getParams();
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


	public ZRequest(final byte[] dataRawArray, final List<ArrayRange> arList) {
		this.dataRawArray = dataRawArray;

		this.arList = arList;

		// 构造参数逻辑考虑同上：
		// 第一个是请求行，不是header，所以-1。headerMap最大存放数量就是size-1，
		// 大多数情况可能不会用到全部的header，所以大多数header都是不会去解析的
		// 所以即使容量设置size-1，也是浪费，尤其是带很多头的请求，可能只会有几分之一会用到
		// 此时设置size-1更是浪费，尤其HashMap容量还会重置为大于此值的2的幂
		this.headerMap = new HashMap<>(arList.size() - 1, 1F);

		parseRequest(this);
	}

	private static ZRequest parseRequest(final ZRequest request) {
		if (CU.isEmpty(request.arList)) {
			return request;
		}

		final String requestLine = new String(PDTL.get().getRequestLineBytes());
		request.original = requestLine;

		parsePath(request);

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
		final Socket socket = SocketTL.get().getSocket();
		if (socket == null) {
			return null;
		}

		final InetSocketAddress inetSocketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
		if (inetSocketAddress == null) {
			return null;
		}

		final InetAddress address = inetSocketAddress.getAddress();
		return address.getHostAddress();
	}

	private static void parsePath(final ZRequest request) {

		final byte[] requestURIBytes = PDTL.get().getRequestURIBytes();

		final int wI = AU.indexOfKeyword(requestURIBytes, STU.Q_BYTE);

		if (wI > -1) {

			final List<byte[]> pa = STU.splitBytes(requestURIBytes, wI + STU.Q_LENGTH, requestURIBytes.length,
					STU.SP_BYTES);

			final List<RequestParam> params = new ArrayList<>(pa.size());
			for (int i = 0; i < pa.size(); i++) {
				final ZRequest.RequestParam requestParam = hParam(new String(pa.get(i)));
				params.add(requestParam);
			}

			request.params = params;
			request.path = URLDecoder.decode(new String(requestURIBytes, 0, wI), Charset.defaultCharset());
			request.queryStringBA = new BA(requestURIBytes, (wI + STU.Q_LENGTH) - 1, requestURIBytes.length);
			request.requestURI = URLDecoder.decode(new String(requestURIBytes), Charset.defaultCharset());
		} else {
			// requestURI中无?符号
			final String requestURI = new String(requestURIBytes);
			final String requestURIDECODE = URLDecoder.decode(requestURI,Charset.defaultCharset());
			request.path = requestURIDECODE;
			request.queryStringBA = null;
			request.requestURI = requestURIDECODE;
		}
	}

	public static byte[] parseRequestURIBytes(final byte[] requestLineBytes) {
		final int si = AU.indexOfKeyword(requestLineBytes, STU.SPACE_BYTE);
		if (si > -1) {
			final int s2i = AU.indexOfKeyword(requestLineBytes, si + 1, STU.SPACE_BYTE);
			if (s2i > (si + 1)) {
				final byte[] pathBytes = Arrays.copyOfRange(requestLineBytes, si + 1, s2i);
				return pathBytes;
			}
		}

		return null;
	}

	public static byte[] parsePATHBytes(final byte[] requestURIBytes) {

		final int wI = AU.indexOfKeyword(requestURIBytes, 0, STU.Q_BYTE);
		if (wI > -1) {
			final byte[] pathBytes = Arrays.copyOfRange(requestURIBytes, 0, wI);
			return pathBytes;
		}

		return requestURIBytes;
	}

	private static ZRequest.RequestParam hParam(final String param) {
		final String[] p0 = param.split(STU.EQUALS);

		final ZRequest.RequestParam requestParam = new ZRequest.RequestParam();
		requestParam.setName(p0[0]);

		if (p0.length >= 2) {

			if (STU.isEmpty(p0[1])) {
				requestParam.setValue(STU.EMPTY);
			} else {

				try {
					final String value = java.net.URLDecoder.decode(p0[1], Task.DEFAULT_CHARSET_NAME);
					requestParam.setValue(value);
				} catch (final UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}

		return requestParam;
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

	// FIXME 2026年6月15日 06:43:20 zhangzhen : 这个方法默认用isNPHNL方法过滤需要解析的，
	// 但是就本程序本身的实现来说，就有问题，比如server.enable.client.qps=true的话,
	// AbstractRequestValidator.validated 中的request.getClientIp和getUserAgent都会导致再次解析
	// 所以，要不直接再加一个配置项：哪些头直接解析。反正这些头已经明确会用到，早晚都要解析，延迟解析还会带来额外开销
	private static void parseHeader(final ZRequest request) {
		final List<ArrayRange> x = request.arList;

		// 第一个是请求行，不是header
		for (int i = 1, size = x.size(); i < size; i++) {

			final ArrayRange arrarRange = x.get(i);

			final int cI = AU.indexOfKeyword(request.dataRawArray,arrarRange.getFrom(), STU.COLON_C_BYTE);

			if (cI <= -1) {
				// FIXME 2026年6月11日 19:45:28 zhangzhen : 头无:符号，应该需要400
				continue;
			}

			// 此时的ba已经去除了前后的空格了,现在只需要去除:符号旁边的空格
			int nT = (cI - arrarRange.getFrom());
			int nTrimSize = 0;
			while ((nT > 0) && (request.dataRawArray[cI] == STU.SPACE_BYTE)) {
				nT--;
				nTrimSize++;
			}

			final int headerNameLength = cI - arrarRange.getFrom() - nTrimSize;

			// FIXME 2026年6月11日 21:39:49 zhangzhen : 注意：下面方法不是完整匹配,
			// 可能有很多误判导致解析了非必要的头而一直不使用浪费cpu和内存
			if (isNPHNL(headerNameLength, request.dataRawArray, arrarRange.getFrom())) {

				final String name = new String(request.dataRawArray, arrarRange.getFrom(), nT);

				final String value = gHV(request.dataRawArray, cI, arrarRange);

				request.headerMap.put(name, value);

				arrarRange.setParsed(true);
			}

		}

	}

	/**
	 * 是否必须解析的头的name的长度，即：以下这些头的长度，
	 *
	 * Host,Expect,Upgrade,Connection,Content-Length,Transfer-Encoding
	 *
	 * @param headerNameLength
	 * @param dataRawArray TODO
	 * @param from TODO
	 * @return
	 */
	private static boolean isNPHNL(final int headerNameLength, final byte[] dataRawArray, final int from) {
		// 暂时只比较几个字符，不比较整个长度的
		return ((headerNameLength == 4) && (dataRawArray[from] == 'H') && (dataRawArray[from + 2] == 's'))
				|| ((headerNameLength == 6) && (dataRawArray[from] == 'E') && (dataRawArray[from + 2] == 'p'))
				|| ((headerNameLength == 7) && (dataRawArray[from] == 'U') && (dataRawArray[from + 2] == 'g'))
				|| ((headerNameLength == 10) && (dataRawArray[from] == 'C') && (dataRawArray[from + 2] == 'n'))
				|| ((headerNameLength == 14) && (dataRawArray[from] == 'C') && (dataRawArray[from + 3] == 't'))
				|| ((headerNameLength == 17) && (dataRawArray[from] == 'T') && (dataRawArray[from + 1] == 'r'));
	}

	private static boolean isNPHNL(final int headerNameLength) {
		return (headerNameLength == 4)
				|| (headerNameLength == 6)
				|| (headerNameLength == 7)
				|| (headerNameLength == 10)
				|| (headerNameLength == 14)
				|| (headerNameLength == 17);
	}

	/**
	 * 解析指定的header，并且返回value
	 *
	 * @param request
	 * @param headerName
	 * @return
	 */
	private static String parseHeaderARHeader(final ZRequest request, final String headerName) {

		final List<ArrayRange> x = request.arList;

		for (int i = 1, size = x.size(); i < size; i++) {
			final ArrayRange arrayRange = x.get(i);
			if (arrayRange.isParsed()) {
				continue;
			}

			final int cI = AU.search(request.dataRawArray, arrayRange.getTo(), STU.COLON_C_BYTES, 1, arrayRange.getFrom());
			if (cI <= -1) {
				continue;
			}

			// 此时的ba已经去除了前后的空格了,现在只需要去除:符号旁边的空格
			int nameTo = (cI - arrayRange.getFrom());
			while ((nameTo > 0) && (request.dataRawArray[cI] == STU.SPACE_BYTE)) {
				nameTo--;
			}

			final String name = new String(request.dataRawArray, arrayRange.getFrom(), nameTo);
			if ((headerName.length() == name.length()) && headerName.equals(name)) {

				final String value = gHV(request.dataRawArray, cI, arrayRange);

				request.headerMap.put(name, value);

				return value;
			}

		}

		return HEADER_PARSED_NO_VALUE;
	}

	private static String gHV(final byte[] dataRawArray, final int cI, final ArrayRange arrayRange) {
		int valueFromIndex = cI + 1;
		while ((valueFromIndex < arrayRange.getTo()) && (dataRawArray[valueFromIndex] == STU.SPACE_BYTE)) {
			valueFromIndex++;
		}

		final int hvLength = arrayRange.getTo() - valueFromIndex;
		// value为null，则设为""
		final String value =
				hvLength == 0
				? STU.EMPTY
				: new String(dataRawArray, valueFromIndex, hvLength);
		return value;
	}

	public String getOriginal() {
		return this.original;
	}

	public String getQueryString() {
		if (this.queryStringBA == null) {
			return null;
		}

		if (this.queryStringCache == null) {
			this.queryStringCache =
					URLDecoder.decode(
							new String(this.queryStringBA.getData(), this.queryStringBA.getFrom(),
									this.queryStringBA.getTo() - this.queryStringBA.getFrom()),
							Charset.defaultCharset());
		}

		return this.queryStringCache;
	}

	public TF getTf() {
		return this.tf;
	}

	public void setTf(final TF tf) {
		this.tf = tf;
	}

	public String getPath() {
		return this.path;
	}

	public List<RequestParam> getParams() {
		return this.params;
	}

	public String getVersion() {
		return this.version;
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

		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder();
			builder.append("RequestParam [name=");
			builder.append(this.name);
			builder.append(", value=");
			builder.append(this.value);
			builder.append("]");
			return builder.toString();
		}

	}

	public byte[] getMethodNameBytes() {
		return this.methodNameBytes;
	}

	public void setMethodNameBytes(final byte[] methodNameBytes) {
		this.methodNameBytes = methodNameBytes;
	}

}
