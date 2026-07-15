package vo.zframework.http.request;

/**
 * 暂时存放Request信息
 *
 * @author zhangzhen
 * @date 2024年2月12日
 *
 */
public class ReqeustInfo {

	@SuppressWarnings("preview")
	public static final ScopedValue<ZRequest> scopedValue = ScopedValue.newInstance();

	public static ZRequest get() {
		return scopedValue.get();
	}

}
