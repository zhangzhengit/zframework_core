package vo.zframework.cache;

import java.nio.charset.StandardCharsets;
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

	public static final byte COLON_C_BYTE = COLON_C;
	public static final byte[] COLON_C_BYTES = {COLON_C_BYTE};
	public static final String EMPTY = "";
	public static final String EQUALS = "=";
	public static final int EQUALS_LENGTH = EQUALS.length();
	public static final char EQUALS_C = '=';
	public static final String SEMICOLON = ";";
	public static final String SAPCE = " ";
	public static final char SPACE_CHAR = ' ';
	public static final char Q_CHAR = '?';
	public static final byte Q_BYTE = Q_CHAR;
	public static final byte SPACE_BYTE = SPACE_CHAR;

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
		return split(ba, ba.length, keyword.getBytes());
	}

	public static List<String> split(final byte[] ba, final int baTo, final byte[] kba) {

		final List<String> ls = new ArrayList<>();

		int from = 0;
		int to = 0;

		int fromIndex = 0;


		while (true) {
			final int i = AU.search(ba, baTo, kba, 1, fromIndex);
			if (i <= -1) {
				ls.add(new String(ba, to + kba.length, baTo - (to + kba.length),StandardCharsets.ISO_8859_1));
				break;
			}

			fromIndex = i + kba.length;
			to = i;

			if (from == to) {
				break;
			}

			ls.add(new String(ba, from, to-from,StandardCharsets.ISO_8859_1));

			from = to + kba.length;
		}

		return ls;
	}

	/**
	 * 对byte[]的split，并且对分割后的行去除前后的空格
	 * 注意：本方法arraycopy可能成为内存热点
	 *
	 * @param bytes        原数组
	 * @param bytesTo      原数组截止位置
	 * @param keywordBytes 分割关键字
	 * @return
	 */
	public static List<byte[]> splitBytes(final byte[] bytes, final int bytesTo, final byte[] keywordBytes) {

		final List<byte[]> ls = new ArrayList<>();

		int from = 0;
		int to = 0;

		int fromIndex = 0;

		while (true) {
			final int i = AU.search(bytes, bytesTo, keywordBytes, 1, fromIndex);
			if (i <= -1) {

				int nFrom = to + keywordBytes.length;
				int nTo = bytesTo;

				while ((nFrom < nTo) && (bytes[nFrom] == SPACE_BYTE)) {
					nFrom++;
				}
				while ((nTo > nFrom) && (bytes[nTo - 1] == SPACE_BYTE)) {
					nTo--;
				}

				ls.add(Arrays.copyOfRange(bytes, nFrom, nTo));

				break;
			}

			fromIndex = i + keywordBytes.length;
			to = i;

			if (from == to) {
				break;
			}

			int nFrom = from;
			int nTo = to;

			while ((nFrom < nTo) && (bytes[nFrom] == SPACE_BYTE)) {
				nFrom++;
			}
			while ((nTo > nFrom) && (bytes[nTo - 1] == SPACE_BYTE)) {
				nTo--;
			}

			ls.add(Arrays.copyOfRange(bytes, nFrom, nTo));

			from = to + keywordBytes.length;
		}

		return ls;
	}

	public static List<ArrayRange> splitBytesAR(final byte[] bytes, final int bytesTo, final byte[] keywordBytes) {

		final List<ArrayRange> ls = new ArrayList<>();

		int from = 0;
		int to = 0;

		int fromIndex = 0;

		while (true) {
			final int i = AU.search(bytes, bytesTo, keywordBytes, 1, fromIndex);
			if (i <= -1) {

				int nFrom = to + keywordBytes.length;
				int nTo = bytesTo;

				while ((nFrom < nTo) && (bytes[nFrom] == SPACE_BYTE)) {
					nFrom++;
				}
				while ((nTo > nFrom) && (bytes[nTo - 1] == SPACE_BYTE)) {
					nTo--;
				}

				ls.add(new ArrayRange(nFrom, nTo, false));

				break;
			}

			fromIndex = i + keywordBytes.length;
			to = i;

			if (from == to) {
				break;
			}

			int nFrom = from;
			int nTo = to;

			while ((nFrom < nTo) && (bytes[nFrom] == SPACE_BYTE)) {
				nFrom++;
			}
			while ((nTo > nFrom) && (bytes[nTo - 1] == SPACE_BYTE)) {
				nTo--;
			}

			ls.add(new ArrayRange(nFrom, nTo, false));

			from = to + keywordBytes.length;
		}

		return ls;
	}

}
