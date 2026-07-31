package vo.zframework.compression;

import com.aayushatharva.brotli4j.encoder.Encoder.Mode;
import com.aayushatharva.brotli4j.encoder.Encoder.Parameters;

/**
 * Brotli压缩选项
 *
 * @author zhangzhen
 * @date 2026年6月25日 09:13:13
 */
public enum BrotliCompressEnum {

	/**
	 * 最高的压缩率，非常耗时，不能用于实时性高的场景
	 */
	BEST_COMPRESSION_RATIO(Parameters.create(11, 24, Mode.GENERIC)),

	;

	private final Parameters parameters;

	BrotliCompressEnum(final Parameters parameters) {
		this.parameters = parameters;
	}

	public Parameters getParameters() {
		return this.parameters;
	}

}
