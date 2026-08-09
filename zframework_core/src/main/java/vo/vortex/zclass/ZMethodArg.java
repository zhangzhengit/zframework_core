package vo.vortex.zclass;

import java.util.Objects;

/**
 * ZMethod的参数
 *
 * @author zhangzhen
 * @date 2023年6月11日
 *
 */
public class ZMethodArg {

	private String type;

	private String name;

	public ZMethodArg(final Class typeClass, final String name) {
		this.type = typeClass.getCanonicalName();
		this.name = name;
	}

	public ZMethodArg(final String type, final String name) {
		this.type = type;
		this.name = name;
	}

	@Override
	public String toString() {
		final String s = this.getType() + " " + this.getName();
		return s;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, type);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ZMethodArg other = (ZMethodArg) obj;
		return Objects.equals(name, other.name) && Objects.equals(type, other.type);
	}

}
