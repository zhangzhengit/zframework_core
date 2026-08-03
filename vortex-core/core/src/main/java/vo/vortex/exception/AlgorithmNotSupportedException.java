package vo.vortex.exception;

/**
 * 算法不支持
 *
 * @author zhangzhen
 * @date 2026年6月26日 20:47:18
 */
public class AlgorithmNotSupportedException extends ZFException {

	private static final long serialVersionUID = 1L;

	public static final String PREFIX = "算法不支持：";

	public AlgorithmNotSupportedException(final String message) {
		super(PREFIX + message);
	}

}
