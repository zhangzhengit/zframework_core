package vo.zframework.enums;

/**
 *
 *
 * @author zhangzhen
 * @date 2026年6月7日 03:17:45
 */
public enum TransferEncodingEnum {

	CHUNKED("chunked", "chunked".getBytes());

	TransferEncodingEnum(final String value, final byte[] valueBytes) {
		this.value = value;
		this.valueBytes = valueBytes;
	}

	public String getValue() {
		return this.value;
	}

	public byte[] getValueBytes() {
		return this.valueBytes;
	}

	private final String value;
	private final byte[] valueBytes;

}
