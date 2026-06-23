package vo.zframework.http;

import java.util.Arrays;

/**
 * byte[]形式的header
 *
 * @author zhangzhen
 * @date 2026年6月11日 05:52:11
 */
public class ZHeader {

	private final byte[] nameBytes;
	private final byte[] valueBytes;

	public ZHeader(final byte[] nameBytes, final byte[] valueBytes) {
		this.nameBytes = nameBytes;
		this.valueBytes = valueBytes;
	}

	public byte[] getValueBytes() {
		return this.valueBytes;
	}

	public byte[] getNameBytes() {
		return this.nameBytes;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ZHeader [nameBytes=");
		builder.append(Arrays.toString(this.nameBytes));
		builder.append(", valueBytes=");
		builder.append(Arrays.toString(this.valueBytes));
		builder.append("]");
		return builder.toString();
	}

}
