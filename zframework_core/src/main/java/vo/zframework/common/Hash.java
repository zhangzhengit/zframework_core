package vo.zframework.common;

import java.util.List;

import com.google.common.hash.Hashing;

/**
 * HASH
 *
 * @author zhangzhen
 * @date 2024年12月7日 上午12:00:24
 *
 */
public class Hash {

	public static String murmur3(final String string) {
		return murmur3(string.getBytes());
	}

	public static String murmur3(final byte[] ba) {
		return Hashing.murmur3_128().newHasher().putBytes(ba).hash().toString();
	}

	public static String murmur3(final List<Byte> bl) {
		if ((bl == null) || bl.isEmpty()) {
			return "";
		}

		final byte[] b = new byte[bl.size()];
		for (int i = 0; i < bl.size(); i++) {
			b[i] = bl.get(i);
		}
		return murmur3(b);
	}

	public static String md5(final byte[] ba) {
		return Hashing.md5().newHasher().putBytes(ba).hash().toString();
	}

	public static String sha256(final byte[] ba) {
		return Hashing.sha256().newHasher().putBytes(ba).hash().toString();
	}

	public static String sha512(final byte[] ba) {
		return Hashing.sha512().newHasher().putBytes(ba).hash().toString();
	}

	public static String goodFastHash(final byte[] ba) {
		return Hashing.goodFastHash(256).newHasher().putBytes(ba).hash().toString();
	}

}
