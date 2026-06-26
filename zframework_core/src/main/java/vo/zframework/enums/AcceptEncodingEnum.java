package vo.zframework.enums;

/**
 * header: Accept-Encoding
 *
 * @author zhangzhen
 * @date 2025年1月2日 下午9:12:58
 *
 */
public enum AcceptEncodingEnum {

	IDENTITY("identity", "identity".getBytes()),

	GZIP("gzip", "gzip".getBytes()),

	DEFLATE("deflate", "deflate".getBytes()),

	BR("br", "br".getBytes()),

	ZSTD("zstd", "zstd".getBytes()),;

	private final String value;
	private final byte[] valueBytes;


	AcceptEncodingEnum(final String value, final byte[] valueBytes) {
		this.value = value;
		this.valueBytes = valueBytes;
	}


	public String getValue() {
		return this.value;
	}

	public byte[] getValueBytes() {
		return this.valueBytes;
	}

}
