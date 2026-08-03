package vo.vortex.enums;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * http-header Connection 选项
 *
 * @author zhangzhen
 * @date 2023年7月4日
 *
 */
public enum ConnectionEnum {

	KEEP_ALIVE("keep-alive","keep-alive".getBytes()),

	CLOSE("close","close".getBytes()),

	;

	private String value;
	private final byte[] valueBytes;

	private final static ConcurrentMap<String, ConnectionEnum> mapV = new ConcurrentHashMap<>();
	static {
		final ConnectionEnum[] v = values();
		for (final ConnectionEnum e : v) {
			mapV.put(e.getValue(), e);
		}

	}

	public static ConnectionEnum valueOfString(final String string) {
		return mapV.get(string);
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	ConnectionEnum(final String value, final byte[] valueBytes) {
		this.value = value;
		this.valueBytes = valueBytes;
	}

	public byte[] getValueBytes() {
		return this.valueBytes;
	}


}
