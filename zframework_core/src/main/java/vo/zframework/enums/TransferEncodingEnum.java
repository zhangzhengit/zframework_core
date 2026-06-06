package vo.zframework.enums;

/**
 *
 *
 * @author zhangzhen
 * @date 2026年6月7日 03:17:45
 */
public enum TransferEncodingEnum {

	CHUNKED("chunked");

	TransferEncodingEnum(final String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	private final String value;

}
