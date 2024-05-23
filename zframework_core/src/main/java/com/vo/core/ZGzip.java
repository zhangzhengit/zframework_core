package com.vo.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import cn.hutool.core.util.ArrayUtil;

/**
 *
 * gzip 压缩
 *
 * @author zhangzhen
 * @date 2023年7月1日
 *
 */
public class ZGzip {

	private static final String DEFAULT_CHARSET = Charset.defaultCharset().displayName();

	public static String decompression(final byte[] ba) {
		if (ArrayUtil.isEmpty(ba)) {
			return null;
		}

		try {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(ba));
			final byte[] buffer = new byte[1024];
			int n;
			while ((n = gzip.read(buffer)) != -1) {
				out.write(buffer, 0, n);
			}

			out.flush();
			out.close();
			gzip.close();

			return new String(out.toByteArray(), DEFAULT_CHARSET).intern();
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static byte[] compress(final String string) {
		if (string == null || string.length() == 0) {
			return null;
		}
		try {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final GZIPOutputStream gzip = new GZIPOutputStream(out);
			gzip.write(string.getBytes(Charset.defaultCharset().displayName()));
			gzip.finish();

			out.close();
			gzip.close();

			return out.toByteArray();

		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
