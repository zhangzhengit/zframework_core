package com.vo.core;

import java.util.List;

import com.google.common.hash.Hashing;

/**
 *
 * @author zhangzhen
 * @date 2024年12月7日 上午12:00:24
 *
 */
public class Hash {

	public static String c(final String string) {
		return c(string.getBytes());
	}

	public static String c(final byte[] ba) {
		return Hashing.murmur3_128().newHasher().putBytes(ba).hash().toString();
	}

	public static String c(final List<Byte> bl) {
		if ((bl == null) || bl.isEmpty()) {
			return "";
		}

		final byte[] b = new byte[bl.size()];
		for (int i = 0; i < bl.size(); i++) {
			b[i] = bl.get(i);
		}
		return c(b);
	}
}
