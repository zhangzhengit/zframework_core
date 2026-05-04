package com.vo.zframework.core;

import java.util.Arrays;

/**
 * 表示formdata中的一个请求项
 *
 * @author zhangzhen
 * @date 2024年12月8日 下午1:59:03
 *
 */
public class FD2 {

	/**
	 * 表示完整的Content-Disposition的一行
	 * 如：
	 * Content-Disposition: form-data; name="file"; filename="123.txt"
	 */
	private String contentDisposition;

	/**
	 * 表示name
	 * 如下例子中本字段值为file
	 * Content-Disposition: form-data; name="file"; filename="123.txt"
	 */
	private String name;

	/**
	 * 表示filename
	 * 如下例子中本字段值为123.txt
	 * Content-Disposition: form-data; name="file"; filename="123.txt"
	 */
	private String fileName;

	/**
	 * 表示Content-Type
	 * 如下例子中本字段值为text/plain
	 * Content-Type: text/plain
	 */
	private String contentType;

	/**
	 * 表示一个完整的文件内容,如需存储等等可直接本字段值
	 */
	private byte[] body;

	/**
	 * 普通表单字段的value
	 */
	private String value;

	public String getContentDisposition() {
		return this.contentDisposition;
	}

	public void setContentDisposition(String contentDisposition) {
		this.contentDisposition = contentDisposition;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFileName() {
		return this.fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getContentType() {
		return this.contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public byte[] getBody() {
		return this.body;
	}

	public void setBody(byte[] body) {
		this.body = body;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "FD2 [contentDisposition=" + this.contentDisposition + ", name=" + this.name + ", fileName=" + this.fileName
				+ ", contentType=" + this.contentType + ", body=" + Arrays.toString(this.body) + ", value=" + this.value + "]";
	}


}
