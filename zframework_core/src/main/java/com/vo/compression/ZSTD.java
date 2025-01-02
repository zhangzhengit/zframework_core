package com.vo.compression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.zip.DeflaterInputStream;
import java.util.zip.DeflaterOutputStream;

import com.github.luben.zstd.Zstd;
import com.vo.cache.AU;
import com.vo.cache.STU;

/**
 *
 * zstd压缩
 *
 * @author zhangzhen
 * @date 2025年1月2日 下午8:37:12
 *
 */
public class ZSTD {

	public static byte[] decompression(final byte[] ba) {
		if (AU.isEmpty(ba)) {
			return null;
		}

		final byte[] buffer = new byte[ba.length];

		final long decompress = Zstd.decompress(buffer, ba);
		return buffer;
	}

	public static byte[] compress(final byte[] ba) {
		if (AU.isEmpty(ba)) {
			return null;
		}

		return Zstd.compress(ba,3);
	}

	public static byte[] compress(final String string) {
		if (STU.isNullOrEmpty(string)) {
			return null;
		}

		byte[] ba = null;
		try {
			ba = string.getBytes(Charset.defaultCharset().displayName());
		} catch (final UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

		return compress(ba);
	}


}
