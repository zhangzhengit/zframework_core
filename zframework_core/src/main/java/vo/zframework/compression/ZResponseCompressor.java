package vo.zframework.compression;

import vo.zframework.http.request.ZRequest;

/**
 * 根据AcceptEncoding 对响应body压缩，优先使用 zstd gzip Deflater，都不支持则不压缩
 *
 * @author zhangzhen
 * @date 2026年6月24日 19:17:03
 */
public class ZResponseCompressor {

	public static byte[] compressByAcceptEncoding(final ZRequest request, final byte[] data) {

		if (request.isSupportZSTD()) {
			return ZSTD.compress(data);
		}
		if (request.isSupportGZIP()) {
			return ZGzip.compress(data);
		}
		if (request.isSupportDEFLATE()) {
			return Deflater.compress(data);
		}
		if (request.isSupportBR()) {
			return Brotli.compress(data);
		}

		return data;
	}

}
