package vo.vortex;

/**
 * Sting[] args 中--k=v形式参数的解析结果
 *
 * @author zhangzhen
 * @date 2025年8月25日
 *
 */
public class ArgR {

	private final String key;
	private final String value;

	public String getKey() {
		return this.key;
	}

	public String getValue() {
		return this.value;
	}

	public ArgR(final String key, final String value) {
		this.key = key;
		this.value = value;
	}

}
