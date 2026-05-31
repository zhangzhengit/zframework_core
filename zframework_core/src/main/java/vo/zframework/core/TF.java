package vo.zframework.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * upload的文件超过配置的阈值后存储到的临时文件信息
 *
 * @author zhangzhen
 * @date 2024年12月19日 下午3:38:55
 *
 */
// FIXME 2024年12月21日 下午8:51:01 zhangzhen : 此类功能和 ZMFile大多都重复了，看能不能少去一步从本类到ZMF的流程？
public class TF {

	private File file;
	private final String tempFilePath;
	private String name;
	private String fileName;
	private String contentType;
	private final OutputStream outputStream;
	private final BufferedOutputStream bufferedOutputStream;

	public TF(final File file, final String tempFilePath, final String name, final String fileName, final OutputStream outputStream,
			final BufferedOutputStream bufferedOutputStream) {
		this.setFile(file);
		this.tempFilePath = tempFilePath;
		this.name = name;
		this.fileName = fileName;
		this.outputStream = outputStream;
		this.bufferedOutputStream = bufferedOutputStream;
	}

	public void write(final byte[] ba, final int off, final int len) {
		try {
			this.getBufferedOutputStream().write(ba, off, len);
			this.getBufferedOutputStream().flush();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void write(final byte[] ba) {
		this.write(ba, 0, ba.length);
	}

	public String getContentType() {
		return this.contentType;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	public void setContentType(final String contentType) {
		this.contentType = contentType;
	}

	public File getFile() {
		return this.file;
	}

	public String getTempFilePath() {
		return this.tempFilePath;
	}

	public String getName() {
		return this.name;
	}

	public String getFileName() {
		return this.fileName;
	}

	public OutputStream getOutputStream() {
		return this.outputStream;
	}

	public BufferedOutputStream getBufferedOutputStream() {
		return this.bufferedOutputStream;
	}

	public void setFile(final File file) {
		this.file = file;
	}

}
