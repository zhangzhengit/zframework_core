package com.vo.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * upload的文件超过配置的阈值后存储到的临时文件信息
 *
 * @author zhangzhen
 * @date 2024年12月19日 下午3:38:55
 *
 */
@Data
@AllArgsConstructor
public class TF {

	private final File file;
	private final String name;
	private final String fileName;
	private String contentType;
	private final OutputStream outputStream;
	private final BufferedOutputStream bufferedOutputStream;

	public TF(final File file, final String name, final String fileName, final OutputStream outputStream,
			final BufferedOutputStream bufferedOutputStream) {
		this.file = file;
		this.name = name;
		this.fileName = fileName;
		this.outputStream = outputStream;
		this.bufferedOutputStream = bufferedOutputStream;
	}

}
