package vo.vortex.exception;

import vo.vortex.http.request.RequestParam;

/**
 * @RequestParam 参数解析异常
 *
 * @author zhangzhen
 * @date 2025年1月19日 下午12:43:18
 *
 */
public class ParsingRequestParamException extends ZFException {

	private static final long serialVersionUID = 1L;
	public static final String PREFIX = "@" + RequestParam.class.getSimpleName() + " 参数解析异常：";

	public ParsingRequestParamException(final String message) {
		super(PREFIX + message);
	}

	public ParsingRequestParamException(final String message, final Integer httpStatus) {
		super(PREFIX + message, httpStatus);
	}

}
