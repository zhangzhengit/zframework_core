package vo.zframework.http;

/**
 * ZConnection ThreadLocal
 *
 * @author zhangzhen
 * @date 2026年6月30日 19:45:38
 */
public class ZConnectionTL {

	private final static ThreadLocal<ZConnection> TL = new ThreadLocal<>();

	public static void set(final ZConnection zConnection) {
		TL.set(zConnection);
	}

	public static ZConnection get() {
		return TL.get();
	}

	public static void remove() {
		TL.remove();
	}

}
