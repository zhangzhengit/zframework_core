package vo.zframework.compression;

import com.github.luben.zstd.Zstd;

import vo.zframework.common.AU;

/**
 *
 * zstd压缩
 *
 * @author zhangzhen
 * @date 2025年1月2日 下午8:37:12
 *
 */
// FIXME 2026年6月24日 16:48:56 zhangzhen : zstd用jni默认打了所有平台的库，jar太大了，试了aircompressor,
/*	简单测试下了，在debian上openjdk25 p8600 4GB thinkpadx200
 *  启动两个jar，唯一不同A用jniB用air，ab -H "Accept-Encoding: zstd"而是，前者qps在5600左右，后者只有4100左右
 *	所以要想jar小，要在打包前修改pom。本次想改zstd是按打出的jar中的lib中的jar大小排序来改的，
 *  不只是想去掉zstd，包括jedis freemarker都要去掉，提供出来一个starter，本工程只提供最简单的rest接口的
 *
 *  aircompressor :
 *
 * 	<dependency>
			<groupId>io.airlift</groupId>
			<artifactId>aircompressor</artifactId>
			<version>0.21</version>
		</dependency>

			public static byte[] compress(final byte[] original) {

		final int maxLen = compressor.maxCompressedLength(original.length);

		final ByteBuffer result = ByteBuffer.allocate(maxLen);
		compressor.compress(ByteBuffer.wrap(original), result);

		result.flip();

		final byte[] compressed = new byte[result.remaining()];
		result.get(compressed);

		return compressed;
	}
 *
 */
public class ZSTD {

	private static final int DEFALUT_COMPRESS_LEVEL = 3;

//	private static final ZstdCompressor compressor = new ZstdCompressor();
//
//	public static byte[] compress(final byte[] original) {
//
//		final int maxLen = compressor.maxCompressedLength(original.length);
//
//		final ByteBuffer result = ByteBuffer.allocate(maxLen);
//		compressor.compress(ByteBuffer.wrap(original), result);
//
//		result.flip();
//
//		final byte[] compressed = new byte[result.remaining()];
//		result.get(compressed);
//
//		return compressed;
//	}

	public static byte[] compress(final byte[] ba) {
		if (AU.isEmpty(ba)) {
			return null;
		}

		return Zstd.compress(ba, DEFALUT_COMPRESS_LEVEL);
	}

}
