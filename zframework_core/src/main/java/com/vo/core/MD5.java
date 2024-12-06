package com.vo.core;

import java.util.List;

import com.google.common.hash.Hashing;

/**
 *
 *
 * @author zhangzhen
 * @date 2024年12月7日 上午12:00:24
 *
 */
public class MD5 {

	public static String c(final List<Byte> bl) {
		if ((bl == null) || bl.isEmpty()) {
			return "";
		}
		
		final byte[] b = new byte[bl.size()];
		for (int i = 0; i < bl.size(); i++) {
			b[i] = bl.get(i);
		}

		final String md5 = Hashing.md5().newHasher().putBytes(b).hash().toString();
		return md5;
	}
}
