package vo.zframework.compression;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import vo.zframework.common.AU;
import vo.zframework.common.STU;

/**
 *
 * gzip 压缩
 *
 * @author zhangzhen
 * @date 2023年7月1日
 *
 */
public class ZGzip {

	private static final int BUFFER_CAPACITY = 1024 * 8;

	private static final String DEFAULT_CHARSET = Charset.defaultCharset().displayName();

	public static String decompression(final byte[] ba) {
		if (AU.isEmpty(ba)) {
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

	public static void compressFile(final Path source, final Path target) {

		try (final InputStream inputStream = Files.newInputStream(source);
			final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
			final OutputStream outputStream = Files.newOutputStream(target);
			final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream) {{
					this.def.setLevel(java.util.zip.Deflater.BEST_COMPRESSION);
			}};
		) {

			final byte[] buffer = new byte[BUFFER_CAPACITY];
			int length;
			while ((length = bufferedInputStream.read(buffer)) != -1) {
				gzipOutputStream.write(buffer, 0, length);
			}

		} catch (final IOException e) {
			e.printStackTrace();
		}

	}

	public static byte[] compress(final byte[] data) {
		if (AU.isEmpty(data)) {
			return null;
		}

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
			gzip.write(data);
			gzip.finish();

		} catch (final IOException e) {
			e.printStackTrace();
		}

		return out.toByteArray();
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
