package vo.vortex.exception;

/**
 *
 * zframework 统一异常类
 *
 * @author zhangzhen
 * @date 2023年10月22日
 *
 */
public class ZFException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public static final int NOT_SET = -1;

	private int httpStatus = NOT_SET;

	public int getHttpStatus() {
		return this.httpStatus;
	}

	public ZFException() {
	}

	public ZFException(final String message) {
		super(message);
	}

	public ZFException(final String message, final int httpStatus) {
		super(message);
		this.httpStatus = httpStatus;
	}

}
