package vo.vortex.http.request;

/**
 *
 *
 * @author zhangzhen
 * @date 2026年7月2日 20:42:15
 */
public class RequestParam {

	private String name;
	private Object value;

	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public void setValue(final Object value) {
		this.value = value;
	}

	public Object getValue() {
		return this.value;
	}

	public RequestParam() {
	}

	public RequestParam(final String name, final Object value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("RequestParam [name=");
		builder.append(this.name);
		builder.append(", value=");
		builder.append(this.value);
		builder.append("]");
		return builder.toString();
	}

}
