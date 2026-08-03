package vo.vortex.compression;

import vo.vortex.core.ZContext;
import vo.vortex.http.request.ZRequest;

/**
 * 根据AcceptEncoding 对响应body压缩，优先使用 zstd gzip Deflater，都不支持则不压缩
 *
 * @author zhangzhen
 * @date 2026年6月24日 19:17:03
 */
public class ZResponseCompressor {

	public static byte[] compressByAcceptEncoding(final ZRequest request, final byte[] data) {

		if (request.isSupportZSTD()) {
			final IZSTD zstd = ZContext.getBean(IZSTD.class);
			return zstd.compress(data);
		}
		// XXX : 已经支持了br了，但是压缩太慢，就不用于实时接口的压缩了，只用于静态资源的预压缩
		if (request.isSupportGZIP()) {
			return ZGzip.compress(data);
		}
		if (request.isSupportDEFLATE()) {
			return Deflater.compress(data);
		}

		return data;
	}

}
