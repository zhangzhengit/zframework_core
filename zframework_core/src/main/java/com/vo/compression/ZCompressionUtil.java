package com.vo.compression;

import java.time.LocalDateTime;

import com.vo.core.ZThreadMap.ZGlobalCacheTypeEnum;

/**
 * 几种压缩方式
 *
 * @author zhangzhen
 * @date 2025年1月2日 下午8:28:34
 *
 */
public class ZCompressionUtil {

	public static void main(final String[] args) {
		test_1();
	}

	public static void test_1() {
		System.out.println(
				Thread.currentThread().getName() + "\t" + LocalDateTime.now() + "\t" + "ZCompressionUtil.test_1()");

		final String string = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

		System.out.println("原字符串 bytes.length = " + string.getBytes().length);

		final byte[] zgip = compression(string.getBytes(),ZCompressionEnum.GZIP);
		System.out.println("zgip.length = " + zgip.length);

		final byte[] deflate = compression(string.getBytes(),ZCompressionEnum.DEFLATE);
		System.out.println("deflate.length = " + deflate.length);

		final byte[] zstd = compression(string.getBytes(),ZCompressionEnum.ZSTD);
		System.out.println("zstd.length = " + zstd.length);

	}

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
