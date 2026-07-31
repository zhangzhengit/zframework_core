package vo.zframework.http.response;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import vo.zframework.enums.HttpStatusEnum;

/**
 * 预分配httpStatus对应的byte[]，避免每次响应都getBytes
 *
 * @author zhangzhen
 * @date 2026年7月6日 04:45:01
 */
public final class HttpStatusBytes {

	private static final int HTTP_STATUS_100 = HttpStatusEnum.HTTP_100.getStatus();

	private static byte[][] CACHE;

	public static byte[] of(final int httpStatus) {
		final byte[] bs = CACHE[httpStatus];
		if (bs != null) {
			return bs;
		}

		final byte[] intToBytes = intToBytes(httpStatus);
		if ((httpStatus <= CACHE.length) && (httpStatus >= HTTP_STATUS_100)) {
			CACHE[httpStatus] = intToBytes;
		}

		return intToBytes;
	}

	static {
		final List<Integer> hsl = Arrays.stream(HttpStatusEnum.values())
							.map(HttpStatusEnum::getStatus)
							.collect(Collectors.toList());

		CACHE = new byte[hsl.get(hsl.size() - 1) + 1][];

		Collections.sort(hsl);

		for (final Integer httpStatus : hsl) {
			final byte[] bytes = intToBytes(httpStatus);
			CACHE[httpStatus] = bytes;
		}

	}

	private static byte[] intToBytes(final int status) {
		final byte b3 = (byte) (48 + (status / 100));
		final byte b2 = (byte) (48 + ((status / 10) % 10));
		final byte b1 = (byte) (48 + (status % 10));
		return new byte[] { b3, b2, b1 };
	}
}