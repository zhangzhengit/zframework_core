package vo.zframework.compression;

import java.io.File;

import vo.zframework.enums.AcceptEncodingEnum;

/**
 *	
 *
 * @author zhangzhen
 * @date 2026年6月25日 21:04:12
 */
public class CF {

	private final File file;
	private final long fileSize;
	private final AcceptEncodingEnum acceptEncodingEnum;

	public File getFile() {
		return this.file;
	}

	public long getFileSize() {
		return this.fileSize;
	}

	public AcceptEncodingEnum getAcceptEncodingEnum() {
		return this.acceptEncodingEnum;
	}

	public CF(final File file, final long fileSize, final AcceptEncodingEnum acceptEncodingEnum) {
		this.file = file;
		this.fileSize = fileSize;
		this.acceptEncodingEnum = acceptEncodingEnum;
	}

}
