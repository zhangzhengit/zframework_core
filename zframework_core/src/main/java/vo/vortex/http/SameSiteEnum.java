package vo.vortex.http;

/**
 *
 * Cookie.SameSite
 *
 * @author zhangzhen
 * @date 2023年7月2日
 *
 */
public enum SameSiteEnum {

	STRICT("Strict","完全禁止第三方 Cookie，跨站点时，任何情况下都不会发送 Cookie"),

	LAX("Lax","允许部分第三方请求携带 Cookie"),

	NONE("None","无论是否跨站都会发送 Cookie"),

	;

	private String value;
	private String description;

	private SameSiteEnum(String value, String description) {
		this.value = value;
		this.description = description;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
