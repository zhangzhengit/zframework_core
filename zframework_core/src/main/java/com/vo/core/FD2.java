package com.vo.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表示formdata中的一个请求项
 *
 * @author zhangzhen
 * @date 2024年12月8日 下午1:59:03
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
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

}
