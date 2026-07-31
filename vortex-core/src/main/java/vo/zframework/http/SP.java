package vo.zframework.http;

import java.util.ArrayList;

/**
 *
 *
 * @author zhangzhen
 * @date 2026年6月20日 07:04:39
 */
public class SP {

	private final ByteArrayKeyWrapper keyWrapper;
	private final ArrayList<Object> vList;

	public SP(final ByteArrayKeyWrapper keyWrapper, final ArrayList<Object> list) {
		this.keyWrapper = keyWrapper;
		this.vList = list;
	}

	public ByteArrayKeyWrapper getKeyWrapper() {
		return this.keyWrapper;
	}

	public ArrayList<Object> getVList() {
		return this.vList;
	}

}
