package vo.zframework.common;

/**
 * 数组带偏移量
 *
 * @author zhangzhen
 * @date 2026年6月20日 13:42:32
 */
public class BA {

	/**
	 * 原数组
	 */
	private final byte[] data;

	/**
	 * 在原数组中的开始位置
	 */
	private final int from;

	/**
	 * 在原数组中的截止位置
	 */
	private final int to;

	public byte[] getData() {
		return this.data;
	}

	public int getFrom() {
		return this.from;
	}

	public int getTo() {
		return this.to;
	}

	public BA(final byte[] data, final int from, final int to) {
		this.data = data;
		this.from = from;
		this.to = to;
	}

	@Override
	public String toString() {
		return "BA = [" + new String(this.data, this.from, this.to - this.from) + "]";
	}

}
