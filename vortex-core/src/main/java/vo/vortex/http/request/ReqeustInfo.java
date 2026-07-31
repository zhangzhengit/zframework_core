package vo.vortex.http.request;

/**
 * 暂时存放Request信息
 *
 * @author zhangzhen
 * @date 2024年2月12日
 *
 */
public class ReqeustInfo {

	private final static ThreadLocal<ZRequest> tl = new ThreadLocal<>();

	public static void set(final ZRequest request) {
		tl.set(request);
	}

	public static ZRequest get() {
		return tl.get();
	}

	public static void remove() {
		tl.remove();
	}

}
