package vo.zframework.http;

import java.util.Arrays;
import java.util.Objects;

/**
 * byte[]作为hashMap的K的包装类
 *
 * @author zhangzhen
 * @date 2026年6月16日 05:18:32
 */
public class ByteArrayKeyWrapper {

	private final byte[] bytes;
	private int hash;

	public ByteArrayKeyWrapper(final byte[] bytes) {
		this.bytes = bytes;
	}

	public byte[] getBytes() {
		return this.bytes;
	}

	@Override
	public int hashCode() {
		if (this.hash == 0) {
			this.hash = Objects.hash(Arrays.hashCode(this.bytes));
		}

		return this.hash;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final ByteArrayKeyWrapper other = (ByteArrayKeyWrapper) obj;
		return Arrays.equals(this.bytes, other.bytes);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ByteArrayKeyWrapper [bytes=");
		builder.append(new String(this.bytes));
		builder.append(", hash=");
		builder.append(this.hash);
		builder.append("]");
		return builder.toString();
	}



}
