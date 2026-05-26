package com.vo.zframework.core;

import java.time.LocalDateTime;

/**
 * 本类规定了 HTTPRequestProcessor 的执行状态，从A状态到B状态
 *
 * @author zhangzhen
 * @date 2026年5月26日 10:54:00
 */
public class HTTPRequestScheduler {

	private static final HttpParseStatusEnum END = HttpParseStatusEnum.PARSE_END;

	HTTPRequestProcessor processor = new HTTPRequestProcessor();

	// FIXME 2026年5月26日 10:55:55 zhangzhen : 在read的while中调用本方法，

	HttpParseStatusEnum process(final HttpParseStatusEnum parseStatusEnum, final PD pd, final byte[] buffer) {
//		System.out.println(LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
//				+ "HTTPRequestScheduler.process()");


		switch (parseStatusEnum) {

		case HttpParseStatusEnum.PARSE_END:
			return this.processor.end(pd, buffer);

		case HttpParseStatusEnum.PARSE_REQUEST_LINE:
			final HttpParseStatusEnum rl = this.parseRL(pd, buffer);
			return rl;

		case HttpParseStatusEnum.CHECK_METHOD:
			return this.checkMethod(pd, buffer);

		case HttpParseStatusEnum.PARSE_HEADER:
			return this.parseHeader(pd, buffer);

			// FIXME 2026年5月26日 11:16:00 zhangzhen : 加入 PARSE_CONTENT_LENGTH

		case HttpParseStatusEnum.PARSE_BODY:
			return this.parseBody(pd, buffer);

		case HttpParseStatusEnum.START:
			this.start(pd, buffer);
			break;

		default:
			break;

		}

		return parseStatusEnum;
	}

	private HttpParseStatusEnum start(final PD pd, final byte[] buffer) {
		final HttpParseStatusEnum startStatus = this.processor.start();
		if (startStatus == END) {
			return this.processor.end(pd, buffer);
		}

		if (startStatus == HttpParseStatusEnum.PARSE_REQUEST_LINE) {
			return this.parseRL(pd, buffer);
		}

		return startStatus;
	}

	private HttpParseStatusEnum parseRL(final PD pd, final byte[] buffer) {
		final HttpParseStatusEnum rquestLineStatus = this.processor.parseRquestLine(pd, buffer);
		if (rquestLineStatus == HttpParseStatusEnum.PARSE_REQUEST_LINE) {
			return HttpParseStatusEnum.PARSE_REQUEST_LINE;
		}

		if (rquestLineStatus == HttpParseStatusEnum.CHECK_METHOD) {
			return this.checkMethod(pd, buffer);
		}

		return rquestLineStatus;
	}

	private HttpParseStatusEnum checkMethod(final PD pd, final byte[] buffer) {
		final HttpParseStatusEnum checkMethodStatus = this.processor.checkMethod(pd, pd.getRequestLine());
		if (checkMethodStatus == END) {
			return this.processor.end(pd, buffer);
		}

		if (checkMethodStatus == HttpParseStatusEnum.PARSE_HEADER) {
			return this.parseHeader(pd, buffer);
		}

		return checkMethodStatus;
	}

	private HttpParseStatusEnum parseHeader(final PD pd, final byte[] buffer) {
		final HttpParseStatusEnum parseHeaderStatus = this.processor.parseHeader(pd, buffer);
		if (parseHeaderStatus == HttpParseStatusEnum.PARSE_BODY) {
			return this.parseBody(pd, buffer);
		}

		return parseHeaderStatus;
	}

	private HttpParseStatusEnum parseBody(final PD pd, final byte[] buffer) {
		final HttpParseStatusEnum parseBodyStatus = this.processor.parseBody(pd, buffer);
		if (parseBodyStatus == END) {
			return this.processor.end(pd, buffer);
		}

		return parseBodyStatus;
	}
}
