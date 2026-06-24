package vo.zframework.compression;

import java.io.IOException;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.encoder.Encoder;

/**
 * Brotli
 *
 * @author zhangzhen
 * @date 2026年6月24日 21:24:26
 */
// FIXME 2026年6月24日 21:47:17 zhangzhen : 压缩太慢了，受不了，待会再看，暂时放在四种的最后，
// 照着github抄的代码 https://github.com/hyperxpro/Brotli4j
public class Brotli {

	public static byte[] compress(final byte[] data) {

		try {
			return Encoder.compress(data);
		} catch (final IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	static {
		Brotli4jLoader.ensureAvailability();
	}
}
