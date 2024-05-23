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
// FIXME 2023年10月26日 下午10:05:48 zhanghen: 这个类支持还有问题，
// 1、现在使用 http 完整信息读取为byte[] 然后整个转为String解析
/*	问题：NioLongConnectionServer.CHARSET 使用default（UTF-8）,上传txt文件正常。上传其他文件如jpg不支持
 * 		NioLongConnectionServer.CHARSET 使用 ISO-8859-1，上传jpg文件正常，但文件名显示不了中文。
 * 	TODO ：
 * 			1 byte[] 一边解析，一边看Content-Type，如果form-data，则再看
 * 				body的 Content-type 来处理：原始byte[] remove掉已转为String的byte[]和
 * 						boundary，剩余的就是文件的byte[]?
 *
 *
 */
public class ZMultipartFile {

	private String name;
	private String originalFilenameString;
	private byte[] content;
	private String contentType;
	private InputStream inputStream;

//	public String getName() {
//		return null;
//	}

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
