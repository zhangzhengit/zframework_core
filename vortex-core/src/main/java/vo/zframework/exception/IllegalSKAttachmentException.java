package vo.zframework.exception;

/**
 * SelectionKey.attachment 非法状态
 *
 * @author zhangzhen
 * @date 2026年5月22日 05:17:54
 */
public class IllegalSKAttachmentException extends ZFException {

	private static final long serialVersionUID = 1L;
	public static final String PREFIX = "SelectionKey.attachment类型异常：";

	public IllegalSKAttachmentException(final String message) {
		super(PREFIX + message);
	}

}
