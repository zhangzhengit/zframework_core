package com.vo.zframework.core;

import java.io.BufferedOutputStream;
import java.io.File;
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
		setFile(file);
		this.tempFilePath = tempFilePath;
		this.name = name;
		this.fileName = fileName;
		this.outputStream = outputStream;
		this.bufferedOutputStream = bufferedOutputStream;
	}

	public String getContentType() {
		return this.contentType;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public void setContentType(String contentType) {
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

	public TF(File file, String tempFilePath, String name, String fileName, String contentType,
			OutputStream outputStream, BufferedOutputStream bufferedOutputStream) {
		setFile(file);
		this.tempFilePath = tempFilePath;
		this.name = name;
		this.fileName = fileName;
		this.contentType = contentType;
		this.outputStream = outputStream;
		this.bufferedOutputStream = bufferedOutputStream;
	}

	public void setFile(File file) {
		this.file = file;
	}

}
