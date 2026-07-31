package vo.zframework.exception;

import vo.zframework.anno.ZPathVariable;

/**
 * @ZPathVariable 解析异常
 *
 * @author zhangzhen
 * @date 2023年11月8日
 *
 */
public class PathVariableException extends ZFException {

	private static final long serialVersionUID = 1L;

	public static final String PREFIX = "@" + ZPathVariable.class.getSimpleName() + " 参数解析异常：";

	public PathVariableException(final String message) {
		super(PREFIX + message);
	}

	public PathVariableException(final String message, final Integer httpStatus) {
		super(PREFIX + message, httpStatus);
	}

}
