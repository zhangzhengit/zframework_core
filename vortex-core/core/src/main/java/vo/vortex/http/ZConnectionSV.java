package vo.vortex.http;

/**
 * ZConnectionSV
 *
 * @author zhangzhen
 * @date 2026年6月30日 19:45:38
 */
public class ZConnectionSV {

	public static final ScopedValue<ZConnection> SV = ScopedValue.newInstance();

	public static ZConnection get() {
		return SV.get();
	}

}
