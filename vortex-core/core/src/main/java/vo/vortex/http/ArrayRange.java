package vo.vortex.http;

/**
 * 标记数组的起止位置
 *
 * @author zhangzhen
 * @date 2026年6月11日 10:22:58
 */
public class ArrayRange {

	private final int from;
	private final int to;

	/**
	 * 本偏移量表示的header是否被解析过了，在延迟解析header时用到
	 */
	private boolean parsed;

	public int getFrom() {
		return this.from;
	}

	public int getTo() {
		return this.to;
	}

	public boolean isParsed() {
		return this.parsed;
	}

	public void setParsed(final boolean parsed) {
		this.parsed = parsed;
	}

	public ArrayRange(final int from, final int to, final boolean parsed) {
		this.from = from;
		this.to = to;
		this.parsed = parsed;
	}

}
