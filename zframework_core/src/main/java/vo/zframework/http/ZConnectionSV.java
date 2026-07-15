package vo.zframework.http;

/**
 * ZConnection
 *
 * @author zhangzhen
 * @date 2026年6月30日 19:45:38
 */
public class ZConnectionSV {

	@SuppressWarnings("preview")
	public static final ScopedValue<ZConnection> scopedValue = ScopedValue.newInstance();

	public static ZConnection get() {
		return scopedValue.get();
	}

}
