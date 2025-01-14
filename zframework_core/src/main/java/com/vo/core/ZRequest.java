package com.vo.core;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.vo.cache.AU;
import com.vo.cache.CU;
import com.vo.cache.STU;
import com.vo.configuration.SCU;
import com.vo.enums.MethodEnum;
import com.vo.http.ZCookie;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表示http 的请求信息
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */

@Data
public class ZRequest {

	public static final String HTTP_11 = "HTTP/1.1";
	public static final String BOUNDARY = "boundary=";
	public static final String MULTIPART_FORM_DATA = "multipart/form-data";
	private static final AtomicLong GZSESSIONID = new AtomicLong(1L);

	// -------------------------------------------------------------------------------------------------
	private List<String> lineList;

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
	MethodEnum methodEnum;

	/**
	 * 完整的path，包含参数的,如：/hello?name=z&age=20
	 */
	String fullpath;

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
	Map<String, String> headerMap;

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
	private final String clientIp;

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

		final String[] array = SCU.split(a, ",");
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

	public String getServerName() {
		final String host = this.getHeaderMap().get(HeaderEnum.HOST.getName());

		final int i = host.indexOf(":");
		if (i > -1) {
			return host.substring(0, i);
		}

		return host;
	}

	public int getServerPort() {

		final String host = this.getHeaderMap().get(HeaderEnum.HOST.getName());

		final int i = host.indexOf(":");
		if (i > -1) {
			final String port = host.substring(i + 1);
			return Integer.parseInt(port);
		}

		return NioLongConnectionServer.DEFAULT_HTTP_PORT;
	}

	public String getRequestURL() {
		final String serverName = this.getServerName();
		return serverName + this.getPath();
	}

	public String getRequestURI() {
		return this.getPath();
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

	private static String gSessionID() {
		final Hasher putString = Hashing.sha256()
				.newHasher()
				.putString(HeaderEnum.Z_SESSION_ID.getName() + System.currentTimeMillis() + ZRequest.GZSESSIONID.getAndDecrement(),
						Charset.defaultCharset());

		final HashCode hash = putString.hash();
		return hash.toString();
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
					ZSessionMap.put(newSession);
					return newSession;
				}
			}
		}

		if (!create) {
			return null;
		}

		final ZSession session = ZRequest.newSession();
		ZSessionMap.put(session);
		return session;
	}

	public static ZSession newSession() {
		final String zSessionID = ZRequest.gSessionID();
		final ZSession session = new ZSession(zSessionID, new Date());

		ZSessionMap.put(session);

		return session;
	}

	public int getContentLength() {
		final String s = this.getHeader(HeaderEnum.CONTENT_LENGTH.getName());
		return s == null ? -1 : Integer.parseInt(s);
	}

	public ZCookie[] getCookies() {

		final String cookisString = this.getHeaderMap().get(HeaderEnum.COOKIE.getName());
		if (STU.isEmpty(cookisString)) {
			return null;
		}

		final String[] a = SCU.split(cookisString, ";");
		final ZCookie[] c = new ZCookie[a.length];
		int cI = 0;
		for (final String s : a) {
			final String[] c1 = SCU.split(s, "=");
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
		if (this.lineList == null) {
			this.lineList = new ArrayList<>();
		}
		return this.lineList;
	}

	public ZRequest(final String[] lineArray) {
		this.lineList = new ArrayList<>(lineArray.length);
		for (final String line : lineArray) {
			this.lineList.add(line);
		}

		this.clientIp = getClientIp0();
		parseRequest(this);
	}

	private static ZRequest parseRequest(final ZRequest request) {
		return parseRequest0(request);
	}

	private static ZRequest parseRequest0(final ZRequest request) {

		if (CU.isEmpty(request.getLineList())) {
			return request;
		}

		// 0 为 请求行
		final String line = request.getLineList().get(0);
		request.setOriginal(line);

		// METHOD 第一个空格前面的是METHOD
		final int methodIndex = line.indexOf(" ");
		if (methodIndex > -1) {
			final String methodS = line.substring(0, methodIndex);
			final MethodEnum me = MethodEnum.valueOfString(methodS);
			// 可能是null，在这里不管，在外面处理，返回405
			request.setMethodEnum(me);
		} else {
			throw new IllegalArgumentException("请求行错误");
		}

		// path
		parsePath(line, request, methodIndex);

		// version
		parseVersion(line, request);

		// paserHeader
		paserHeader(request);

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

	// FIXME 2024年12月19日 下午12:23:50 zhangzhen : 这个改掉
	private static String getClientIp0() {

		final SocketChannel socketChannel = Task.SCTL.get();
		if (socketChannel == null) {
			return null;
		}
		// FIXME 2023年11月16日 下午2:47:38 zhanghen: ab 测试这里可能取不到,修复掉
		final Socket socket = socketChannel.socket();
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


	private static void parsePath(final String s, final ZRequest request, final int methodIndex) {
		final int pathI = s.indexOf(" ", methodIndex + 1);
		if (pathI <= -1) {
			throw new IllegalArgumentException("请求行错误：找不到path");
		}

		final String fullPath = s.substring(methodIndex  + 1, pathI);

		final int wenI = fullPath.indexOf("?");
		if (wenI > -1) {
			request.setQueryString(fullPath.substring(("?".length() + wenI) - 1));

			final Set<RequestParam> paramSet = Sets.newHashSet();
			final String param = fullPath.substring("?".length() + wenI);
			final String simplePath = fullPath.substring(0,wenI);

			try {
				request.setPath(java.net.URLDecoder.decode(simplePath, Task.DEFAULT_CHARSET_NAME));
			} catch (final UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			final String[] paramArray = param.split(Task.SP);
			for (final String p : paramArray) {
				final String[] p0 = p.split("=");
				final ZRequest.RequestParam requestParam = new ZRequest.RequestParam();
				requestParam.setName(p0[0]);
				if (p0.length >= 2) {
					try {
						final String v = STU.isEmpty(p0[1]) ? Task.EMPTY_STRING
								: java.net.URLDecoder.decode(p0[1], Task.DEFAULT_CHARSET_NAME);
						requestParam.setValue(v);
					} catch (final UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				} else {
					requestParam.setValue(Task.EMPTY_STRING);
				}

				paramSet.add(requestParam);
			}

			request.setParamSet(paramSet);

		} else {
			try {
				request.setPath(java.net.URLDecoder.decode(fullPath, Task.DEFAULT_CHARSET_NAME));
			} catch (final UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		request.setFullpath(fullPath);
	}

	private static void parseHost(final String line, final ZRequest request) {

	}

	private static void parseVersion(final String requestLine, final ZRequest request) {
		final int hI = requestLine.lastIndexOf("HTTP/");
		if (hI > -1) {
			final String version = requestLine.substring(hI);
			if (!HTTP_11.equalsIgnoreCase(version)) {
				// FIXME 2024年12月19日 下午1:41:45 zhangzhen : ab 命令测试会走到异常，要不要抛异常以后再看
				//				throw new IllegalArgumentException("请求行错误：HTTP版本错误,本服务器支持HTTP/1.1");
			}
			request.setVersion(version);
		} else {
			throw new IllegalArgumentException("请求行错误：找不到HTTP版本");
		}
	}

	private static void paserHeader(final ZRequest request) {
		final List<String> x = request.getLineList();
		final HashMap<String, String> hm = new HashMap<>(16, 1F);
		for (int i = x.size() - 1; i > 0; i--) {
			final String l = x.get(i);
			if (Task.EMPTY_STRING.equals(l)) {
				continue;
			}

			final int k = l.indexOf(":");
			if (k > -1) {
				final String key = l.substring(0, k).trim();
				final String value = l.substring(k + 1).trim();
				// CONTENT_LENGTH 头在bodyreader.readHeader时已经校验过了，在此肯定非负的整数
				hm.put(key, value);
			}
		}
		request.setHeaderMap(hm);
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class RequestParam {

		private String name;
		private Object value;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ZHeader {

		private String name;
		private String value;
	}

}
