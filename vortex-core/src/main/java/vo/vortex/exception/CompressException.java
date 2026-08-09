package vo.vortex.exception;

/**
 * 压缩异常
 *
 * @author zhangzhen
 * @date 2026年6月25日 16:55:13
 */
public class CompressException extends ZFException {

	private static final long serialVersionUID = 1L;

	public static final String PREFIX = "压缩异常：";

	public CompressException(final String message) {
		super(PREFIX + message);
	}
}
