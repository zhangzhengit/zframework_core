package com.vo.exception;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.vo.anno.ZComponent;
import com.vo.cache.STU;
import com.vo.common.CR;
import com.vo.core.ReqeustInfo;
import com.vo.core.ZContext;
import com.vo.core.ZRequest;
import com.vo.http.ZCookie;
import com.vo.log.core.ZLog2;
import com.vo.validator.ZFException;

/**
 * @ZControllerAdvice 的默认处理方法，如果 @ZExceptionHandler 定义的
 *                    的处理方法中，没有任何一个匹配异常，则使用本类来处理。
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@ZComponent
public class ZControllerAdviceThrowable {

	private static final ZLog2 LOG = ZLog2.getInstance();

	// FIXME 2023年11月4日 下午7:31:39 zhanghen: XXX
	/*
	 * 此方法还待定，是否直接提示具体报错信息给前端？要不要写得模糊一点？如：服务器内部错误
	 */
	public CR throwZ(final Throwable throwable) {
		final String m = findCausedby(throwable);
		final ZControllerAdviceThrowableConfigurationProperties conf = ZContext.getBean(ZControllerAdviceThrowableConfigurationProperties.class);

		final ZRequest request = ReqeustInfo.get();
		if (request != null) {
			final ZCookie zsessionid = request.getZSESSIONID();
			LOG.error("请求出错：path={},clientIp={},ZSESSIONID={}", request.getRequestURI(), request.getClientIp(),
					zsessionid == null ? "无" : zsessionid.getValue());
		}
		// FIXME 2025年1月20日 下午9:09:12 zhangzhen : 如果reqeust
		// ==null，则要打印的ERROR也没法打印了，因为要打印的内容都在request中

		return CR.error(conf.getErrorCode(), m);
	}

	public static String getHostName() {
		InetAddress inetAddress = null;
		try {
			inetAddress = InetAddress.getLocalHost();
		} catch (final UnknownHostException e) {
			e.printStackTrace();
		}
		final String hostName = inetAddress.getHostName();

		return hostName;
	}

	public static Integer findHttpStatus(final Throwable e) {
		if (e instanceof ZFException) {
			return ((ZFException) e).getHttpStatus();
		}

		final Throwable cause = e.getCause();
		if (cause == null) {
			return null;
		}

		if (cause instanceof ZFException) {
			return ((ZFException) cause).getHttpStatus();
		}

		return null;
	}

	public static String findCausedby(final Throwable e) {
		if (e instanceof ZFException) {
			return ((ZFException) e).getMessagezf();
		}

		if ((e.getCause() != null) && (e.getCause() instanceof ZFException)) {
			return ((ZFException) e.getCause()).getMessagezf();
		}

		if (e.getCause() != null) {
			return e.getCause().getClass().getCanonicalName() + STU.COLON + e.getCause().getMessage();
		}

		return e.getClass().getCanonicalName() + STU.COLON + e.getMessage();
	}

}
