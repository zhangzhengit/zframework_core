package vo.vortex.route;

/**
 * APIRoute执行结果
 *
 * @author zhangzhen
 * @date 2026年7月16日 16:34:40
 */
public class APIRouteR {

	private final boolean matched;

	private final Object rv;

	public APIRouteR(final Object rv) {
		this.matched = true;
		this.rv = rv;
	}

	public APIRouteR(final boolean matched) {
		this.matched = matched;
		this.rv = null;
	}

	public APIRouteR(final boolean matched, final Object rv) {
		this.matched = matched;
		this.rv = rv;
	}

	public boolean isMatched() {
		return this.matched;
	}

	public Object getRv() {
		return this.rv;
	}

}
