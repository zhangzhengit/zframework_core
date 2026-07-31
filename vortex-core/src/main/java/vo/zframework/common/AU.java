package vo.zframework.common;

import java.util.Arrays;

/**
 * 数组相关
 *
 * @author zhangzhen
 * @date 2024年12月21日 下午9:57:53
 *
 */
public class AU {


	public static <T> boolean isNotEmpty(final T[] array) {
		return (array != null) && (array.length > 0);
	}

	public static boolean isEmpty(final byte[] array) {
		return (array == null) || (array.length == 0);
	}

	public static boolean isNotEmpty(final byte[] array) {
		return !isEmpty(array);
	}

	public static <T> boolean isEmpty(final T[] array) {
		return (array == null) || (array.length == 0);
	}

	public static boolean isEmpty(final boolean[] ba) {
		return (ba == null) || (ba.length == 0);
	}

	/**
	 * 搜索关键字出现在byte[]中的位置
	 *
	 * @param data
	 * @param keyword    	要搜索的关键字
	 * @param iN          	第几次出现的位置
	 * @param fromIndex 	从ba数组开始搜索的位置
	 * @return
	 */
	public static int search(final byte[] data,final String keyword, final int iN, final int fromIndex) {
		if ((keyword == null) || keyword.isEmpty()) {
			return -1;
		}

		return search(data, data.length, keyword.getBytes(), iN, fromIndex);
	}

	public static int search(final byte[] data, final int dataTo, final byte[] keyword, final int iN, final int fromIndex) {
		if (isEmpty(keyword)) {
			return -1;
		}

		int findN = 0;
		for (int i = fromIndex; i < dataTo; i++) {
//		for (int i = fromIndex; i < data.length; i++) {
			boolean find = true;
			if (i >= ((dataTo - keyword.length) + 1)) {
//			if (i >= ((data.length - keyword.length) + 1)) {
				find = false;
				break;
			}
			for (int k = 0; k < keyword.length; k++) {
				if (data[i + k] != keyword[k]) {
					find = false;
					break;
				}
			}

			// FIXME 2025年12月4日 下午8:23:49 zhangzhen: 下面if先注释，因为发现了bug了，不知道当时为什么这么写了
			// 当前字节的上面是\r
//			if (find && (i > 0) && (ba[i - 1] == '=')) {
//				find = false;
//				break;
//			}
			if (find) {
				findN++;
				if (findN >= iN) {
					return i;
				}
			}
		}

		return -1;
	}

	public static int indexOfKeyword(final byte[] bytes, final byte keyword) {
		return indexOfKeyword(bytes, 0, keyword);
	}

	public static int indexOfKeyword(final byte[] bytes,final int bytesFrom, final byte keyword) {
		for (int i = bytesFrom; i < bytes.length; i++) {
			if (bytes[i] == keyword) {
				return i;
			}
		}
		return -1;
	}

	public static byte[] trim(final byte[] bytes) {
		if(isEmpty(bytes)) {
			return null;
		}

		int from = 0;
		while((from < bytes.length) && (bytes[from] == STU.SPACE_BYTE)) {
			from++;
		}

		int to = bytes.length-1;
		while((to > 0) && (bytes[to] == STU.SPACE_BYTE)) {
			to--;
		}

		if (to >= from) {
			return Arrays.copyOfRange(bytes, from, to + 1);
		}

		return null;
	}

	/**
	 * 多个byte[]合并为一个
	 *
	 * @param bs
	 * @return
	 */
	public static byte[] concat(final byte[]... bs) {
		int length = 0;
		for (final byte[] b : bs) {
			length += b.length;
		}

		final byte[] r = new byte[length];
		int i = 0;
		for (final byte[] b : bs) {
			System.arraycopy(b, 0, r, i, b.length);
			i += b.length;
		}

		return r;
	}

}
