package vo.zframework.html;

import java.io.File;
import java.io.InputStream;

/**
 *
 *
 * @author zhangzhen
 * @date 2026年6月19日 06:07:50
 */
public class FIS {

	private final InputStream inputStream;
	private final File file;

	public FIS(final InputStream inputStream, final File file) {
		this.inputStream = inputStream;
		this.file = file;
	}

	public InputStream getInputStream() {
		return this.inputStream;
	}

	public File getFile() {
		return this.file;
	}

}
