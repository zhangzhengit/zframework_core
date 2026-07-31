package vo.zframework.exception;

/**
 *
 * 资源异常
 *
 * @author zhangzhen
 * @date 2023年10月31日
 *
 */
public class ResourceNotExistException extends ZFException {

	private static final long serialVersionUID = 1L;

	public static final String PREFIX = "资源不存在：";

	public ResourceNotExistException(final String message) {
		super(PREFIX + message);
	}

	public ResourceNotExistException(final String messagezf, final Integer httpStatus) {
		super(PREFIX + messagezf, httpStatus);
	}

}
