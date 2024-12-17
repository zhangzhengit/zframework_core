package com.vo.core;

import java.io.InputStream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接收 multipart/form-data 上传的文件，专指文件。
 *
 * @author zhangzhen
 * @date 2023年10月26日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ZMultipartFile {

	private String name;
	private String originalFilenameString;
	private byte[] content;
	private String contentType;
	private InputStream inputStream;

	/**
	 * 返回上传的文件的名称，指文件名
	 *
	 * @return
	 *
	 */
	public String getOriginalFilename() {
		return this.originalFilenameString;
	}

	/**
	 * 返回文件的Content-Type
	 *
	 * @return
	 *
	 */
	public String getContentType() {
		return this.contentType;
	}

	/**
	 * 返回上传的文件是否empty
	 *
	 * @return
	 *
	 */
	public boolean isEmpty() {
		return this.getSize() > 0;
	}

	/**
	 * 返回上传文件的字节数
	 *
	 * @return
	 *
	 */
	public long getSize() {
		return this.getBytes().length;
	}

	/**
	 * 返回上传文件的字节数组
	 *
	 * @return
	 */
	public byte[] getBytes() {
		return this.getContent();
	}

	public InputStream getInputStream() {
		return this.inputStream;
	}
}
