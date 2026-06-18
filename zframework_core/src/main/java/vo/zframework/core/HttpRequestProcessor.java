package vo.zframework.core;

import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import vo.zframework.anno.ZComponent;
import vo.zframework.cache.AU;
import vo.zframework.cache.STU;
import vo.zframework.configuration.ServerConfigurationProperties;
import vo.zframework.http.ZControllerMap;
import vo.zframework.http.ZRMethod;

/**
 * 默认的http请求处理流程，用户可自定义类继承并覆盖相关方法实现自定义的解析流程
 *
 * @author zhangzhen
 * @date 2024年12月22日 下午4:57:13
 *
 */
// FIXME 2026年5月26日 08:56:17 zhangzhen : 其他method继续写

// FIXME 2026年5月26日 17:01:06 zhangzhen : 提供一个类似RequestValidatorAdapter的类，给用户自定义实现自定义的http请求解析
// FIXME 2026年5月30日 20:36:48 zhangzhen : 截止现在，此类都是认为请求都是正常的合法的，没怎么判断非法情况
// 记得判断，任何一个点都要判断校验
@ZComponent
public class HttpRequestProcessor {

	// FIXME 2026年5月30日 16:00:48 zhangzhen : 这个里面所有的search，如果返回-1了 ，可以设置一个当前length A,下次search从A开始，就少做很多重复的无用功

	private static final String HTTP = "HTTP/";

	private static final  byte[] HTTP_BYTES = HTTP.getBytes();

	private static final byte[] CONTENT_LENGTH_BYTES = HeaderEnum.CONTENT_LENGTH.getName().getBytes();

	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES= ZContext.getBean(ServerConfigurationProperties.class);

	private static final String[] METHOD = SERVER_CONFIGURATIONPROPERTIES.getMethod().split(",");

	private static final int UPLOAD_FILE_TO_TEMP_SIZE = SERVER_CONFIGURATIONPROPERTIES.getUploadFileToTempSize();

	private static final ArrayList<byte[]> x = new ArrayList<>();

	static {
		for (final String m : METHOD) {
			if (STU.isNotEmpty(m)) {
				x.add(m.getBytes());
			}
		}
		x.trimToSize();
		x.sort(Comparator.comparing(a -> a.length));
	}

	/**
	 * 用于在一次http请求读取解析之前初始化和校验一些服务器限制等等
	 * 本类默认为校验 server.qps
	 * @param pd TODO
	 *
	 * @return
	 */
	// FIXME 2026年5月26日 09:09:55 zhangzhen : 校验server.qps 不该放在此类？应该写成固定的代码不该给用户实现？
	public HttpParseStatusEnum start(final PD pd) {
		if (ZServer.allow()) {
			return HttpParseStatusEnum.PARSE_REQUEST_LINE;
		}

		final ZResponse response = ReU.gResponse429(SERVER_CONFIGURATIONPROPERTIES.getQpsExceedMessage(), false);
		pd.setException(response);
		return HttpParseStatusEnum.EXCEPTION;
	}


	/**
	 * 解析请求行
	 *
	 * @param array
	 */
	public HttpParseStatusEnum parseRquestLine(final PD pd, final ZArray array) {
		final byte[] requestLineBA = parseRequestLine(pd, array);
		if (pd.getRequestLineEndIndex() <= -1) {
			// FIXME 2026Ln : 没找到，继续读(buffer容量太小)？还是抛异常(恶意制造的不合法请求)？
			return HttpParseStatusEnum.PARSE_REQUEST_LINE;
		}

		pd.setRequestLineBytes(requestLineBA);

		// FIXME 2026年5月26日 09:56:12 zhangzhen : 除了METHOD，还看版本，不支持响应505
		return HttpParseStatusEnum.CHECK_METHOD;
	}

	/**
	 * 从请求行中校验METHOD，支持则继续下一步[PARSE_HEADER]，不支持则响应405并且结束
	 *
	 * @param pd
	 * @param requestLineBytes TODO
	 * @return
	 */
	public HttpParseStatusEnum checkMethod(final PD pd, final byte[] requestLineBytes) {

		final int i = AU.indexOfKeyword(requestLineBytes, 0, STU.SPACE_BYTE);
		if (i <= -1) {
			// 到此，requestLine 里连一个空格都没有
			final ZResponse response = ReU.response400("requestLine错误", false);
			pd.setException(response);
			return HttpParseStatusEnum.EXCEPTION;
		}

		final byte[] methodBytes = Arrays.copyOfRange(requestLineBytes, 0, i);

		if (HttpRequestProcessor.methodSupportBytes(methodBytes)) {
			pd.setMethodNameBytes(methodBytes);

			return HttpParseStatusEnum.CHECK_URI;
		}

		// 到此，METHOD 不支持
		final ZResponse response = ReU.response405(new String(methodBytes), false);
		pd.setException(response);

		return HttpParseStatusEnum.EXCEPTION;
	}

	public static boolean methodSupportBytes(final byte[] methodBytes) {
		final int mbLength = methodBytes.length;
		for (int i = 0; i < x.size(); i++) {
			final byte[] b = x.get(i);
			if ((mbLength == b.length)) {
				for (int k = 0; k < b.length; k++) {
					if (b[k] != methodBytes[k]) {
						return false;
					}
				}

				return true;
			}
		}

		return false;
	}

	/**
	 * 校验请求行中的URI是否存在
	 *
	 * @param pd
	 * @return
	 */
	public HttpParseStatusEnum checkURI(final PD pd) {

		final byte[] requestURIBytes = ZRequest.parseRequestURIBytes(pd.getRequestLineBytes());
		pd.setRequestURIBytes(requestURIBytes);

		final byte[] pathBytes = ZRequest.parsePATHBytes(requestURIBytes);

		// 1、精确匹配
		final ZRMethod zrMethod = ZControllerMap.getMethodByMethodEnumAndPath(pd.getMethodNameBytes(), pathBytes);
		if (zrMethod != null) {
			// 精确匹配到了
			pd.setZrMethod(zrMethod);
			return HttpParseStatusEnum.CHECK_VERSION;
		}

		// 2、URI正则匹配
		final String path = new String(pathBytes);
		final ZRMethod matcheZRMethod = Task.getMatcheMethod(pd.getMethodNameBytes(), path);
		if (matcheZRMethod == null) {

			// 3、继续URI正则匹配，依然没匹配到，但用非请求的METHOD和URI精确匹配到了，响应405
			final ZRMethod matchWithServerMethod = Task.matchWithServerMethod(pd.getMethodNameBytes(), path, pathBytes);
			if (matchWithServerMethod != null) {
				final ZResponse response405 = ReU.response405(new String(pd.getMethodNameBytes()), false);
				pd.setException(response405);
				return HttpParseStatusEnum.EXCEPTION;
			}
			pd.setZrMethod(matchWithServerMethod);

			// FIXME 2026年5月28日 17:44:46 zhangzhen : 到此是否继续用非请求的METHOD和URI正则继续匹配？
			// 4、响应404
			final ZResponse response404 = ReU.response404(path, false);
			pd.setException(response404);
			return HttpParseStatusEnum.EXCEPTION;
		}
		pd.setZrMethod(matcheZRMethod);

		// FIXME 2026年5月26日 15:16:13 zhangzhen : 校验协议版本 http1.1

		// 继续下一步，校验httpVERSION
		return HttpParseStatusEnum.CHECK_VERSION;
	}

	public HttpParseStatusEnum checkVersion(final PD pd, final byte[] requestLineBytes) {

		final int hI = AU.search(requestLineBytes, requestLineBytes.length, HTTP_BYTES, 1, 4);
		if (hI <= -1) {
			throw new IllegalArgumentException("请求行错误：找不到HTTP版本");
		}

		final byte[] versionBytes = Arrays.copyOfRange(requestLineBytes, hI, requestLineBytes.length);
		if (!isHttp11(versionBytes)) {
			// FIXME 2024年12月19日 下午1:41:45 zhangzhen : ab 命令测试会走到异常，要不要抛异常以后再看
						//				throw new IllegalArgumentException("请求行错误：HTTP版本错误,本服务器支持HTTP/1.1")
		}

		pd.setHttpVersionBytes(versionBytes);

		return HttpParseStatusEnum.PARSE_HEADER;
	}


	private static boolean isHttp11(final byte[] httpVersionBytes) {
		return (ZRequest.HTTP_11_BYTES.length == httpVersionBytes.length)
				&& Arrays.equals(ZRequest.HTTP_11_BYTES, httpVersionBytes);
	}

	/**
	 * 解析header，默认实现为:
	 * 1、校验header字符合法性
	 * 2、某些header只能出现一次，如：host
	 * 3、单个header大小 HTTP_431
	 *
	 * @param pd
	 * @param array
	 * @return
	 */
	public HttpParseStatusEnum parseHeader(final PD pd, final ZArray array) {

		final int headerEndIndex = AU.search(array.getRawArray(), array.length(), STU.CRLFCRLF_BYTES, 1, pd.getSearchHeaderEndIndexFromIndex());
		if (headerEndIndex <= -1) {

			// 本次read后没找到，则设置下次搜索开始位置为当前已读取的长度，即：从下次读取的内容的开头开始搜
			pd.setSearchHeaderEndIndexFromIndex(array.length());

			// header 没结束，继续读
			return HttpParseStatusEnum.PARSE_HEADER;
		}

		final int headerLength = headerEndIndex - (pd.getRequestLineEndIndex() + STU.CRLF_LENGTH);
		if (headerLength > ZRequest.requestHeaderSizeLimit) {
			final ZResponse response = ReU.response431("", false);
			pd.setException(response);
			return HttpParseStatusEnum.EXCEPTION;
		}

		pd.setHeaderEndIndex(headerEndIndex);

		final long contentLength = gContentLength(array, pd);
		if (contentLength <= -1) {
			// 无Content-Length，直接跳到结束
			return HttpParseStatusEnum.PARSE_END;
		}

		if (pd.getContentLength() <= -1) {
			// Content-Length 不合法，异常结束
			final ZResponse response = ReU.response400("Content-Length错误", false);
			pd.setException(response);
			return HttpParseStatusEnum.EXCEPTION;
		}
		// FIXME 2026年5月30日 09:54:00 zhangzhen : parseHeader应该在这里就执行，今早解析今早发现错误，记得在这里加了以后，把
		// end 流程中的parseHeader避免掉，整个流程只解析一次

		// FIXME 2026年5月26日 15:27:57 zhangzhen : 校验 Transfer-Encoding

		return HttpParseStatusEnum.PARSE_BODY;
	}

	// FIXME 2026年5月26日 10:52:08 zhangzhen : 要不要加一个content-type判断？是否上传文件？

	public HttpParseStatusEnum parseBody(final PD pd, final ZArray array) {

		// FIXME 2026年5月26日 09:34:11 zhangzhen : 这个相当复杂，先写外面的调用者，根据此类每个方法的返回值来跳转到不同状态

		final HttpParseStatusEnum parseStatusEnum = parseBody(pd.getBufferedInputStream(), array, pd);

		return parseStatusEnum;
	}

	/**
	 * 解析成功完成后的最后状态
	 *
	 * @param array TODO
	 */
	public HttpParseStatusEnum end(final PD pd, final ZArray array) {

		final ZRequest request = HttpRequestParser.parse(array.getRawArray(), array.length(), pd.getHeaderEndIndex());

		request.setMethodNameBytes(pd.getMethodNameBytes());

		pd.setRequest(request);

		return HttpParseStatusEnum.START;
	}

	private static byte[] parseRequestLine(final PD pd, final ZArray array) {
		final int requestLineEndIndex = AU.search(array.getRawArray(), array.length(), STU.CRLF_BYTES, 1, 3);

		if (requestLineEndIndex <= -1) {
			return null;
		}

		pd.setRequestLineEndIndex(requestLineEndIndex);

		final byte[] requestLineBytes = Arrays.copyOfRange(array.getRawArray(), 0, requestLineEndIndex);
		return requestLineBytes;
	}

	private static long gContentLength(final ZArray array, final PD pd) {
		final int clIndex = AU.search(array.getRawArray(), array.length(), CONTENT_LENGTH_BYTES, 1,
				pd.getRequestLineEndIndex() + STU.CRLF.length());
		if (clIndex > -1) {
			final int clEIndex = AU.search(array.getRawArray(), array.length(), STU.CRLF_BYTES, 1, clIndex);
			if (clEIndex > clIndex) {

				final int cI = AU.indexOfKeyword(array.getRawArray(), clIndex, STU.COLON_C_BYTE);
				if (cI > -1) {

					// FIXME 2026年6月15日 22:21:31 zhangzhen : arraycopy和trim都可以去掉，继续用偏移量
					final byte[] lBAX = Arrays.copyOfRange(array.getRawArray(), cI + 1, clEIndex);
					final byte[] trimx = AU.trim(lBAX);

					final long contentLength = parseContentLength(trimx, 0, trimx.length);

					pd.setContentLength(contentLength);

					return contentLength;
				}
			}
		}

		return -1;
	}

	public static long parseContentLength(final byte[] bytes, final int offset, final int length) {
	    long value = 0;
	    for (int i = offset; i < length; i++) {
	        final byte b = bytes[offset + i];
	        if ((b < '0') || (b > '9')) {
	            throw new NumberFormatException("Invalid Content-Length");
	        }
	        value = (value * 10) + (b - '0');
	        // 可选：检查 overflow
	        if (value < 0) {
				throw new NumberFormatException("Value too large");
			}
	    }
	    return value;
	}

	public static HttpParseStatusEnum parseBody(final BufferedInputStream bufferedInputStream,
			final ZArray array, final PD pd) {

		if (pd.getContentLength() >= (UPLOAD_FILE_TO_TEMP_SIZE * 1024)) {
			return writeToTempFile(bufferedInputStream, array, pd);
		}

		if ((pd.getHeaderEndIndex() + STU.CRLFCRLF.length() + pd.getContentLength()) == array.length()) {
			return HttpParseStatusEnum.PARSE_END;
		}

		return HttpParseStatusEnum.PARSE_BODY;
	}

	public static HttpParseStatusEnum writeToTempFile(final BufferedInputStream bufferedInputStream, final ZArray array,
			final PD pd) {

		final String randomFileName = "file_" + System.nanoTime();
		final TF tf = HttpRequestParser.saveToTempFile(randomFileName, randomFileName, randomFileName);

		final byte[] bodyOne = Arrays.copyOfRange(array.getRawArray(), pd.getHeaderEndIndex() + STU.CRLF.length(),
				array.length());
		tf.write(bodyOne);

		// FIXME 2026年6月1日 22:25:08 zhangzhen : 这个的array需要删除headerEnd之后的部分

		// 先判断一下 bodyOne 是否已包含了完整的请求
		if ((pd.getHeaderEndIndex() + STU.CRLFCRLF.length() + pd.getContentLength()) == array.length()) {
			return HttpParseStatusEnum.PARSE_END;
		}

		// 不再放入array,而是写入文件
		final int fbc = 1024 * 8;
		final byte[] buffer = new byte[fbc];
		int fRC = 0;

		while (true) {
			final int r1 = ZServer.read0(bufferedInputStream, buffer);

			if (r1 <= -1) {
				break;
			}

			fRC += r1;
			tf.write(buffer, 0, r1);

			if ((pd.getHeaderEndIndex() + STU.CRLFCRLF.length() + pd.getContentLength())
					== (array.length() + fRC)) {

				final Fm fm = HttpRequestParser.hFM(array);
				final String boundary = fm.getBoundary();

				try {
					HttpRequestParser.readFileNameAndContentType(tf);
					HttpRequestParser.removeNB(tf, boundary, array);
					pd.setTf(tf);
				} finally {
					HttpRequestParser.closeTFStream(tf);
				}

				return HttpParseStatusEnum.PARSE_END;
			}

		}

		// 为了编译通过，返回PARSE_END
		return HttpParseStatusEnum.PARSE_END;
	}

}
