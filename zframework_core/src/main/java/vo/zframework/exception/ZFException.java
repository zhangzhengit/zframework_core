package vo.zframework.exception;

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

	private int httpStatus;

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
