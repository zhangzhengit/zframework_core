package vo.zframework.cache;

/**
 * 标记数组的起止位置
 *
 * @author zhangzhen
 * @date 2026年6月11日 10:22:58
 */
public class ArrayRange {

	private final int from;
	private final int to;

	public int getFrom() {
		return this.from;
	}

	public int getTo() {
		return this.to;
	}

	public ArrayRange(final int from, final int to) {
		this.from = from;
		this.to = to;
	}

}
