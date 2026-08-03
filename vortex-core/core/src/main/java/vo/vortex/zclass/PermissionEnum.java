package vo.vortex.zclass;

/**
 * 权限修饰符
 *
 * @author zhangzhen
 * @date 2025年9月23日
 * 
 */
public enum PermissionEnum {

	PUBLIC("public"), PRIVATE("private"), PROTECTED("protected"), DEFAULT("");

	private final String v;

	public String getV() {
		return v;
	}

	private PermissionEnum(final String v) {
		this.v = v;
	}

}
