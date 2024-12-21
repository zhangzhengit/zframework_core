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
// FIXME 2024年12月21日 下午8:51:01 zhangzhen : 此类功能和 ZMFile大多都重复了，看能不能少去一步从本类到ZMF的流程？
public class TF {

	private final File file;
	private final String tempFilePath;
	private final String name;
	private final String fileName;
	private String contentType;
	private final OutputStream outputStream;
	private final BufferedOutputStream bufferedOutputStream;

	public TF(final File file, final String tempFilePath, final String name, final String fileName, final OutputStream outputStream,
			final BufferedOutputStream bufferedOutputStream) {
		this.file = file;
		this.tempFilePath = tempFilePath;
		this.name = name;
		this.fileName = fileName;
		this.outputStream = outputStream;
		this.bufferedOutputStream = bufferedOutputStream;
	}

}
