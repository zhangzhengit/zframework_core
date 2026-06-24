package vo.zframework.http;

import vo.zframework.http.request.ZRequest;
import vo.zframework.http.response.ZResponse;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年7月2日
 *
 */
public class ZHttpContext {

	private static final ThreadLocal<ZResponse> RESPONSE = new ThreadLocal<>();

	private static final ThreadLocal<ZRequest> REQUEST = new ThreadLocal<>();

	public static void setZResponse(final ZResponse response) {
		ZHttpContext.RESPONSE.set(response);
	}

	public static void setZRequest(final ZRequest request) {
		ZHttpContext.REQUEST.set(request);
	}

	public static ZRequest getZRequest() {
		return ZHttpContext.REQUEST.get();
	}

	public static ZResponse getZResponse() {
		return ZHttpContext.RESPONSE.get();
	}

	public static ZResponse getZResponseAndRemove() {
		final ZResponse v = ZHttpContext.RESPONSE.get();
		ZHttpContext.RESPONSE.remove();
		return v;
	}

}
