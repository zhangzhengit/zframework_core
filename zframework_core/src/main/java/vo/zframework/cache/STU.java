package vo.zframework.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import vo.zframework.core.ZRC;

/**
 * String相关
 *
 * @author zhangzhen
 * @date 2024年12月18日 下午2:53:38
 *
 */
public class STU {

	public static final String CR = "\r";
	public static final String LF = "\n";
	public static final String CRLF = "\r\n";
	public static final int CRLF_LENGTH = CRLF.getBytes().length;
	public static final byte[] CRLF_BYTES = STU.CRLF.getBytes();
	public static final String CRLFCRLF = "\r\n\r\n";
	public static final byte[] CRLFCRLF_BYTES = CRLFCRLF.getBytes();
	public static final String COLON = ":";
	public static final byte[] COLON_BYTES = STU.COLON.getBytes();
	public static final int COLON_LENGTH = COLON.getBytes().length;
	public static final char COLON_C = ':';
	public static final String EMPTY = "";
	public static final String EQUALS = "=";
	public static final char EQUALS_C = '=';
	public static final String SEMICOLON = ";";
	public static final String SAPCE = " ";


	public static String toLowerCase(final String string) {
		return ZRC.singleton().computeIfAbsent(string, () -> string.toLowerCase());
	}

	public static boolean isNull(final String string) {
		return (string == null);
	}

	public static boolean isEmpty(final String string) {
		return (string == null) || (string.isEmpty());
	}

	public static boolean isNotNull(final String string) {
		return (string != null);
	}

	public static boolean isNotEmpty(final String string) {
		return (string != null) && !string.isEmpty();
	}

	public static boolean isNotNullAndNotEmpty(final String string) {
		return (string != null) && !string.isEmpty();
	}

	public static boolean isNullOrEmpty(final String string) {
		return (string == null) || string.isEmpty();
	}

	public static boolean isNullOrEmptyOrBlank(final String string) {
		return (string == null) || string.isEmpty() || string.trim().isEmpty();
	}

	public static boolean hasContent(final String string) {
		return !isNullOrEmptyOrBlank(string);
	}

	public static boolean isPureAscii(final String str) {
		if ((str == null) || str.isEmpty()) {
			return true;
		}
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) > 127) {
				return false;
			}
		}
		return true;
	}

	/**
	 * split，没仔细测，暂时写为一个工具类方法，只是为了替代split("\r\n")而写的，因为它会走正则
	 *
	 * @param ba
	 * @param keyword
	 * @return
	 */
	public static List<String> split(final byte[] ba, final String keyword) {

		final List<String> ls = new ArrayList<>();

		int from = 0;
		int to = 0;

		int fromIndex = 0;

		final byte[] kba = keyword.getBytes();

		while (true) {
			final int i = AU.search(ba, kba, 1, fromIndex);
			if (i <= -1) {
				ls.add(new String(Arrays.copyOfRange(ba, to + keyword.length(), ba.length)));
				break;
			}

			fromIndex = i + keyword.length();
			to = i;

			if (from == to) {
				break;
			}

			ls.add(new String(Arrays.copyOfRange(ba, from, to)));

			from = to + keyword.length();
		}

		return ls;
	}

}
