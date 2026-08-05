package vo.vortex.http.request;

/**
 * 存放/传递ZRequest信息
 *
 * @author zhangzhen
 * @date 2024年2月12日
 *
 */
public class ZReqeustSV {

	public static final ScopedValue<ZRequest> SV = ScopedValue.newInstance();

	public static ZRequest get() {
		return SV.get();
	}

}
