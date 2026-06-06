package vo.zframework.cache;

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

		return search(data, keyword.getBytes(), iN, fromIndex);
	}

	public static int search(final byte[] data, final byte[] keyword, final int iN, final int fromIndex) {
		if (isEmpty(keyword)) {
			return -1;
		}

		int findN = 0;
		for (int i = fromIndex; i < data.length; i++) {
			boolean find = true;
			if (i >= ((data.length - keyword.length) + 1)) {
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

}
