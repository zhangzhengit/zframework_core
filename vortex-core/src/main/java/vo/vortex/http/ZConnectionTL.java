package vo.vortex.http;

/**
 * ZConnection
 *
 * @author zhangzhen
 * @date 2026年6月30日 19:45:38
 */
public class ZConnectionTL {

	private final static ThreadLocal<ZConnection> tl = new ThreadLocal<>();

	public static void set(final ZConnection connection) {
		tl.set(connection);
	}

	public static ZConnection get() {
		return tl.get();
	}

	public static void remove() {
		tl.remove();
	}

}
