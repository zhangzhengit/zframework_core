package vo.zframework.compression;

import java.nio.file.Path;

/**
 * zstd接口
 *
 * @author zhangzhen
 * @date 2026年7月4日 19:53:06
 */
public interface IZSTD {

	int DEFALUT_COMPRESS_LEVEL = 3;
	// FIXME 2026年6月25日 14:53:48 zhangzhen : 设为22，firefox无法解压
	int BEST_COMPRESS_LEVEL = 18;

	byte[] compress(byte[] data);

	byte[] compress(byte[] data, int level);

	void compressFile(Path source, Path target);

}