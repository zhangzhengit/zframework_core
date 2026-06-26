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

	private final Integer httpStatus;

	public Integer getHttpStatus() {
		return this.httpStatus;
	}

	public ZFException() {
		this.httpStatus = null;
	}

	public ZFException(final String message) {
		super(message);
		this.httpStatus = null;
	}

	public ZFException(final String message, final Integer httpStatus) {
		super(message);
		this.httpStatus = httpStatus;
	}

}
