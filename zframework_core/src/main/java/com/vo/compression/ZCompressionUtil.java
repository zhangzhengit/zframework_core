package com.vo.compression;

/**
 * 几种压缩方式
 *
 * @author zhangzhen
 * @date 2025年1月2日 下午8:28:34
 *
 */
public class ZCompressionUtil {

	public static byte[] compression(final byte[] ba, final ZCompressionEnum compressionEnum) {

		switch (compressionEnum) {
		case GZIP:
			return ZGzip.compress(ba);

		case DEFLATE:
			return Deflater.compress(ba);

		case ZSTD:
			return ZSTD.compress(ba);

		default:
			break;
		}

		// FIXME 2025年1月2日 下午9:08:08 zhangzhen : 暂时这样，记得支持br
		throw new IllegalArgumentException("不支持的压缩方式,compressionEnum = " + compressionEnum);
	}

}
