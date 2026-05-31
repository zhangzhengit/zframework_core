package vo.zframework.cache;

/**
 * 数组相关
 *
 * @author zhangzhen
 * @date 2024年12月21日 下午9:57:53
 *
 */
public class AU {

	/**
	 * 取一个数组的特征
	 * 
	 * @param ba
	 * @return
	 */
	public static String tezheng(final byte[] ba) {
		if (AU.isEmpty(ba)) {
			return null;
		}

		long sum = 0L;
		long difference = Long.MAX_VALUE;
		long sumJi = 0L;
		long sumOu = 0L;
		long zeroC = 0L;
		long oneC = 0L;
		for (int i = 0; i < ba.length; i++) {
			sum += ba[i];
			difference -= ba[i];
			if ((i % 2) == 1) {
				sumJi += ba[i];
			} else {
				sumOu += ba[i];
			}
			if (ba[i] == 0) {
				zeroC++;
			} else if (ba[i] == 1) {
				oneC++;
			}
		}

		return "T-" + ba.length + '-' + sum + '-' + difference + '-' + sumJi + '-' + sumOu + '-' + zeroC + '-' + oneC;
	}
	
	public static <T> boolean isNotEmpty(final T[] array) {
		return (array != null) && (array.length > 0);
	}

	public static boolean isEmpty(final byte[] array) {
		return (array == null) || (array.length == 0);
	}

	public static <T> boolean isEmpty(final T[] array) {
		return (array == null) || (array.length == 0);
	}

	public static boolean isEmpty(final boolean[] ba) {
		return (ba == null) || (ba.length == 0);
	}

}
