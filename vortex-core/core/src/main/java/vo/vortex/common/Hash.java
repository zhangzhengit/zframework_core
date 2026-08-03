package vo.vortex.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * HASH
 *
 * @author zhangzhen
 * @date 2024年12月7日 上午12:00:24
 *
 */
public class Hash {

	private static final String SHA_512 = "SHA-512";


	public static String sha512(final byte[] bytes) {

		final byte[] r = DIGEST_THREAD_LOCAL.get().digest(bytes);

		final StringBuilder hex = new StringBuilder(r.length * 2);
		for (final byte b : r) {
			final String s = Integer.toHexString(b & 0xFF);
			if (s.length() == 1) {
				hex.append('0');
			}
			hex.append(s);
		}

		return hex.toString();
	}

	private static final ThreadLocal<MessageDigest> DIGEST_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
		try {
			return MessageDigest.getInstance(SHA_512);
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException(SHA_512 + "不可用", e);
		}
	});
}
