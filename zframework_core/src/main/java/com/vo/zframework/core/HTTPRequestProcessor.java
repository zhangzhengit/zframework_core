package com.vo.zframework.core;

import java.util.Arrays;

import com.vo.zframework.cache.STU;
import com.vo.zframework.configuration.ServerConfigurationProperties;
import com.vo.zframework.enums.MethodEnum;
import com.vo.zframework.http.ZControllerMap;
import com.vo.zframework.http.ZRMethod;

/**
 * 默认的http请求处理流程
 *
 * 正常流程：
 * 1、GET
 * 		开始 > 校验请求行 > 校验METHOD > 校验请求头 > 结束
 * 2、POST
 * 		开始 > 校验请求行 > 校验METHOD > 校验请求头 > 校验body > 结束
 *
 * @author zhangzhen
 * @date 2024年12月22日 下午4:57:13
 *
 */
// FIXME 2026年5月26日 08:56:17 zhangzhen : 其他method继续写
// FIXME 2026年5月26日 10:46:28 zhangzhen : 要不要简单一点，本类任何方法不通过，都是响应后直接close，
// 免得处理不好脏数据导致一堆bug出力不讨好，尤其是解析body尤其是上传文件这种低频大数据量的操作，新建一个tcp连接
// 的开销相对来说完全可忽略
public class HTTPRequestProcessor {

	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES= ZContext.getBean(ServerConfigurationProperties.class);

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

		// FIXME 2026年5月26日 10:58:54 zhangzhen : 抛异常
		// FIXME 2026年5月26日 12:19:02 zhangzhen : 不应该在此类中响应，此类应该只负责解析，
		// 把ZResponse放在pd中，在外面根据状态EXCEPTION来响应异常信息
		final ZResponse response = ReU.gResponse429(pd.getSocket(), SERVER_CONFIGURATIONPROPERTIES.getQpsExceedMessage(), false);
		pd.setException(response);
		return HttpParseStatusEnum.EXCEPTION;
	}


	/**
	 * 从read出的buffer中解析请求行
	 * @param buffer
	 * @param array TODO
	 */
	public HttpParseStatusEnum parseRquestLine(final PD pd, final byte[] buffer, final ZArray array) {
		final String requestLine = ZServer.parseRequestLine(buffer, pd);
		if (pd.getRequestLineIndex() <= -1) {
			// FIXME 2026年5月26日 09:32:24 zhangzhen : 没找到，继续读(buffer容量太小)？还是抛异常(恶意制造的不合法请求)？
			return HttpParseStatusEnum.PARSE_REQUEST_LINE;
		}
		pd.setRequestLine(requestLine);

		return HttpParseStatusEnum.CHECK_METHOD;
		// FIXME 2026年5月26日 09:56:12 zhangzhen : 除了METHOD，还看版本，不支持响应505
	}

	/**
	 * 从请求行中校验METHOD，支持则继续下一步[PARSE_HEADER]，不支持则响应405并且结束
	 *
	 * @param pd
	 * @param requestLine
	 *
	 * @return
	 */
	public HttpParseStatusEnum checkMethod(final PD pd, final String requestLine) {

		final int i = requestLine.indexOf(STU.SAPCE);
		if (i <= -1) {
			// 到此，requestLine 里连一个空格都没有
			final ZResponse response = ReU.response400(pd.getSocket(), "requestLine错误", false);
			pd.setException(response);
			return HttpParseStatusEnum.EXCEPTION;
		}

		final String method = requestLine.substring(0, i);

		final String serverMethod = SERVER_CONFIGURATIONPROPERTIES.getMethod();
		final String[] ma = serverMethod.split(",");
		for (final String m : ma) {
			if (method.equals(m)) {
				final MethodEnum methodEnum = MethodEnum.valueOfMethodStringUpper(method);
				pd.setMethodEnum(methodEnum);
				return HttpParseStatusEnum.CHECK_URI;
			}
		}

		// 到此，METHOD 不支持
		final ZResponse response = ReU.response405(pd.getSocket(), method, false);
		pd.setException(response);

		// FIXME 2026年5月26日 09:23:02 zhangzhen : END后，本次请求的请求行之后的数据怎么处理？
		// 要不直接closeSocket?
		return HttpParseStatusEnum.EXCEPTION;

	}

	/**
	 * 校验请求行中的URI是否存在
	 *
	 * @param pd
	 * @param requestLine
	 * @return
	 */
	public HttpParseStatusEnum checkURI(final PD pd, final String requestLine) {

		final String path = ZRequest.parsePATH(requestLine);

		final ZRMethod zrMethod = ZControllerMap.getMethodByMethodEnumAndPath(pd.getMethodEnum(), path);
		if (zrMethod != null) {
			// 精确匹配到了
			return HttpParseStatusEnum.PARSE_HEADER;
		}

		final ZRMethod matcheZRMethod = Task.getMatcheMethod(pd.getMethodEnum(), path);
		// 继续正则匹配，依然没匹配到，响应404
		if (matcheZRMethod == null) {
			final ZResponse response404 = ReU.response404(pd.getSocket(), path, false);
			pd.setException(response404);
			return HttpParseStatusEnum.EXCEPTION;
		}

		// FIXME 2026年5月26日 15:16:13 zhangzhen : 校验协议版本 http1.1

		// 继续下一步，解析HEADER
		return HttpParseStatusEnum.PARSE_HEADER;
	}

	/**
	 * 解析header，默认实现为:
	 * 1、校验header字符合法性
	 * 2、某些header只能出现一次，如：host
	 * 3、单个header大小 HTTP_431
	 *
	 * @param pd
	 * @param buffer
	 * @param array TODO
	 * @return
	 */
	public HttpParseStatusEnum parseHeader(final PD pd, final byte[] buffer, final ZArray array) {
		// FIXME 2026年5月26日 09:28:58 zhangzhen : 这个方法要校验的太多了，先忽略掉,默认返回 PARSE_BODY
		// 用正确的http工具来请求测试
		// 以后再制造不合法的请求来测试本方法


		final int headerEndIndex = ZServer.getHeaderEndIndex(array, pd);
		if (headerEndIndex <= -1) {
			// header 没结束，继续读
			return HttpParseStatusEnum.PARSE_END;
		}

		final String contentLength = ZServer.gContentLength(array, pd);
		if(STU.isEmpty(contentLength)) {
			// 无Content-Length，直接跳到结束
			return HttpParseStatusEnum.PARSE_END;
		}

		if (pd.getContentLength() <= -1) {
			// Content-Length 不合法，异常结束
			final ZResponse response = ReU.response400(pd.getSocket(), "Content-Length错误", false);
//			final ZResponse response = ReU.response400(pd.getSocket(), HeaderEnum.CONTENT_LENGTH.getName() + "错误", false);
			pd.setException(response);
			return HttpParseStatusEnum.EXCEPTION;
		}

		// FIXME 2026年5月26日 15:27:57 zhangzhen : 校验 Transfer-Encoding

		return HttpParseStatusEnum.PARSE_BODY;
	}

	// FIXME 2026年5月26日 10:52:08 zhangzhen : 要不要加一个content-type判断？是否上传文件？

	public HttpParseStatusEnum parseBody(final PD pd, final byte[] buffer, final ZArray array) {

		// FIXME 2026年5月26日 09:34:11 zhangzhen : 这个相当复杂，先写外面的调用者，根据此类每个方法的返回值来跳转到不同状态

		final HttpParseStatusEnum body = ZServer.parseBody(pd.getSocket(), pd.getBufferCapacity(), pd.getBufferedInputStream(), array, pd);

		return body;
//		return HttpParseStatusEnum.PARSE_END;
	}

	/**
	 * 解析成功完成后的最后状态
	 * @param array TODO
	 */
	public HttpParseStatusEnum end(final PD pd, final byte[] buffer, final ZArray array) {
//		System.out.println(
//				LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "HTTPRequestProcessor.end()");
		// FIXME 2026年5月26日 09:30:41 zhangzhen : 这里应该写：重置ZArray readCount
		// 等等所有资源，等待下一个请求到来

		final ZRequest request = ZServer.parse(buffer, pd.getSocket());
//		System.out.println("request = ");
//		System.out.println(request);

		pd.setRequest(request);

		return HttpParseStatusEnum.START;
	}

}
