package com.vo.compression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.zip.DeflaterInputStream;
import java.util.zip.DeflaterOutputStream;

import com.vo.cache.AU;
import com.vo.cache.STU;

/**
 * Deflater压缩
 *
 * @author zhangzhen
 * @date 2025年1月2日 下午8:32:51
 *
 */
public class Deflater {

	private static final String DEFAULT_CHARSET = Charset.defaultCharset().displayName();

	public static String decompression(final byte[] ba) {
		if (AU.isEmpty(ba)) {
			return null;
		}

		try {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();

			final DeflaterInputStream deflater = new DeflaterInputStream(new ByteArrayInputStream(ba));
			final byte[] buffer = new byte[1024];
			int n;
			while ((n = deflater.read(buffer)) != -1) {
				out.write(buffer, 0, n);
			}

			out.flush();
			out.close();
			deflater.close();

			return new String(out.toByteArray(), DEFAULT_CHARSET).intern();
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static byte[] compress(final byte[] data) {
		final ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();

		try (DeflaterOutputStream deflaterOut = new DeflaterOutputStream(byteArrayOut,
				new java.util.zip.Deflater(java.util.zip.Deflater.BEST_COMPRESSION))) {
			deflaterOut.write(data);
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return byteArrayOut.toByteArray();
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
