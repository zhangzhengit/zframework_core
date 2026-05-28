package com.vo.zframework.core;

/**
 * 本类规定了 HTTPRequestProcessor 的执行状态，从A状态到B状态
 *
 * @author zhangzhen
 * @date 2026年5月26日 10:54:00
 */
public class HTTPRequestScheduler {

	private static final HttpParseStatusEnum START = HttpParseStatusEnum.START;

	private static final HttpParseStatusEnum EXCEPTION = HttpParseStatusEnum.EXCEPTION;

	/**
	 * 支持结束的最终状态，是【正常】结束，不是异常结束
	 */
	private static final HttpParseStatusEnum END = HttpParseStatusEnum.PARSE_END;

	private final HTTPRequestProcessor processor = new HTTPRequestProcessor();

	// FIXME 2026年5月26日 10:55:55 zhangzhen : 在read的while中调用本方法，

	HttpParseStatusEnum process(final HttpParseStatusEnum parseStatusEnum, final PD pd, final ZArray array) {
//		System.out.println(LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
//				+ "HTTPRequestScheduler.process()");


		switch (parseStatusEnum) {

		case HttpParseStatusEnum.PARSE_END:
			return this.processor.end(pd, array);

		case HttpParseStatusEnum.PARSE_REQUEST_LINE:
			return this.parseRL(pd, array);

		case HttpParseStatusEnum.CHECK_METHOD:
			return this.checkMethod(pd, array);

		case HttpParseStatusEnum.PARSE_HEADER:
			return this.parseHeader(pd, array);

		case HttpParseStatusEnum.PARSE_BODY:
			return this.parseBody(pd, array);

		case HttpParseStatusEnum.START:
			return this.start(pd, array);

		default:
			break;

		}

		return parseStatusEnum;
	}

	private HttpParseStatusEnum start(final PD pd, final ZArray array) {
		final HttpParseStatusEnum startStatus = this.processor.start(pd);
		if (startStatus == END) {
			return this.processor.end(pd, array);
		}

		if (startStatus == HttpParseStatusEnum.PARSE_REQUEST_LINE) {
			return this.parseRL(pd, array);
		}

		if (startStatus == START) {
			// FIXME 2026年5月26日 11:50:20 zhangzhen : START ，暂时这么写，防止save
			// action自己把这个if给删了，待会再改saveaction设置
			return startStatus;
		}

		if (startStatus == EXCEPTION) {
			return EXCEPTION;
		}

		return startStatus;
	}

	private HttpParseStatusEnum parseRL(final PD pd, final ZArray array) {
		final HttpParseStatusEnum rquestLineStatus = this.processor.parseRquestLine(pd, array);
		if (rquestLineStatus == HttpParseStatusEnum.PARSE_REQUEST_LINE) {
			return HttpParseStatusEnum.PARSE_REQUEST_LINE;
		}

		if (rquestLineStatus == HttpParseStatusEnum.CHECK_METHOD) {
			return this.checkMethod(pd, array);
		}

		return rquestLineStatus;
	}

	private HttpParseStatusEnum checkMethod(final PD pd, final ZArray array) {
		final HttpParseStatusEnum checkMethodStatus = this.processor.checkMethod(pd, pd.getRequestLine());
		if (checkMethodStatus == END) {
			return this.processor.end(pd, array);
		}

		if (checkMethodStatus == HttpParseStatusEnum.PARSE_HEADER) {
			return this.parseHeader(pd, array);
		}
		if (checkMethodStatus == HttpParseStatusEnum.CHECK_URI) {
			return this.checkURI(pd, array);
		}

		return checkMethodStatus;
	}

	private HttpParseStatusEnum checkURI(final PD pd, final ZArray array) {
		final HttpParseStatusEnum parseHeaderStatus = this.processor.checkURI(pd, pd.getRequestLine());
		if (parseHeaderStatus == HttpParseStatusEnum.PARSE_HEADER) {
			return this.parseHeader(pd, array);
		}

		return parseHeaderStatus;
	}

	private HttpParseStatusEnum parseHeader(final PD pd, final ZArray array) {
		final HttpParseStatusEnum parseHeaderStatus = this.processor.parseHeader(pd, array);
		if (parseHeaderStatus == HttpParseStatusEnum.PARSE_BODY) {
			return this.parseBody(pd, array);
		}

		if (parseHeaderStatus == HttpParseStatusEnum.PARSE_END) {
			return this.processor.end(pd, array);
		}

		return parseHeaderStatus;
	}

	private HttpParseStatusEnum parseBody(final PD pd, final ZArray array) {
		final HttpParseStatusEnum parseBodyStatus = this.processor.parseBody(pd, array);
		if (parseBodyStatus == END) {
			return this.processor.end(pd, array);
		}

		return parseBodyStatus;
	}


}
