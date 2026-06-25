package vo.zframework.compression;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream;
import com.aayushatharva.brotli4j.encoder.Encoder;

import vo.zframework.api.StaticResourcespreCompressionService;
import vo.zframework.exception.CompressException;

/**
 * Brotli
 *
 * @author zhangzhen
 * @date 2026年6月24日 21:24:26
 */
// FIXME 2026年6月24日 21:47:17 zhangzhen : 压缩太慢了，受不了，待会再看，暂时放在四种的最后，
// 照着github抄的代码 https://github.com/hyperxpro/Brotli4j
public class Brotli {

	private static final int BUFFER_CAPACITY = 1024 * 8;

	private static final AtomicBoolean AVAILABLE = new AtomicBoolean(true);

	public static boolean isAvailable() {
		return AVAILABLE.get();
	}

	public static void compressFile(final Path source, final Path target) {

		if (isAvailable()) {
			throw new CompressException(Brotli.class.getName() + "压缩不可用");
		}

		try (final InputStream inputStream = Files.newInputStream(source);
			final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
			final OutputStream outputStream = Files.newOutputStream(target);
			final BrotliOutputStream brotliOutputStream = new BrotliOutputStream(outputStream,
					BrotliCompressEnum.BEST_COMPRESSION_RATIO.getParameters());
		) {

			final byte[] buffer = new byte[BUFFER_CAPACITY];
			int length;
			while ((length = bufferedInputStream.read(buffer)) != -1) {
				brotliOutputStream.write(buffer, 0, length);
			}

			// FIXME 2026年6月25日 11:33:20 zhangzhen : 这部分分离出去，违反单一了
			final Path brp = target.resolveSibling(String.valueOf(target.getFileName())
					.replace(StaticResourcespreCompressionService.TEMP_BR, StaticResourcespreCompressionService.BR));
			Files.move(target, brp);

		} catch (final IOException e) {
			e.printStackTrace();
		}

	}

	public static byte[] compress(final byte[] data, final BrotliCompressEnum bce) {
		try {
			return Encoder.compress(data, bce.getParameters());
		} catch (final IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	static {
		try {
			Brotli4jLoader.ensureAvailability();
		} catch (final Throwable ingore) {
			AVAILABLE.set(false);
		}
	}
}
