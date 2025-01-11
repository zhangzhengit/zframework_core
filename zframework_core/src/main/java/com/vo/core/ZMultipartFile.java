package com.vo.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 接收 multipart/form-data 上传的文件，专指文件。
 *
 * @author zhangzhen
 * @date 2023年10月26日
 *
 */
@Data
@AllArgsConstructor
public class ZMultipartFile {

	private final String name;
	private final String tempFilePath;
	private final String originalFilenameString;
	private final byte[] content;
	private final boolean isTempFile;
	private final String contentType;
	private final InputStream inputStream;

	/**
	 * 返回上传的文件的名称，指上传的文件的原始文件名，如：123.txt abcd.jpg 等等
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
	 * 返回文件内容是否empty
	 *
	 * @return
	 *
	 */
	public boolean isEmpty() {
		return this.getSize() > 0;
	}

	/**
	 * 返回文件的字节数
	 *
	 * @return
	 *
	 */
	public long getSize() {
		return this.content.length;
	}

	/**
	 * 返回文件的全部内容，适用于比较小的文件，一次性读取文件全部内容到内存中
	 *
	 * 注意：如果 isTempFile 方法返回true，
	 * 		则本方法返回为null，此时请用 getInputStream 方法来读取文件
	 *
	 * @return
	 */
	public byte[] getContent() {
		return this.content;
	}

	/**
	 * 获取上传的文件的输入流，适用于比较大的文件分批读取。
	 * 如果接收到的文件比较小(由 @see ServerConfigurationProperties.uploadFileToTempSize 控制)
	 * 用 getContent 来读取更方便
	 *
	 * 注意：使用本方法后记得把inputStream给close掉，
	 * 		并且使用 BufferedInputStream 来包装此类来读取，否则可能出现读取不完整的情况
	 *
	 * @return	返回接收到的文件的输入流，不管文件大小都会返回一个InputStream对象
	 */
	public InputStream getInputStream() {
		return this.inputStream;
	}

	/**
	 * 本对象是否写入了临时文件,
	 * 由 @see ServerConfigurationProperties.uploadFileToTempSize 控制
	 * 超过(>)此值则本方法返回true，不超(<=)此值返回false
	 *
	 * @return
	 */
	public boolean isTempFile() {
		return this.isTempFile;
	}

	/**
	 * 把上传的文件复制到新的目标File
	 *
	 * @param dest 目标文件，不存在会自动创建
	 * @throws IOException
	 */
	public void transferTo(final File dest) throws IOException {
		if (!dest.exists()) {
			dest.createNewFile();
		}

		final InputStream inputStream2 = this.getInputStream();
		final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream2);

		final FileOutputStream fileOutputStream = new FileOutputStream(dest);
		final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

		final byte[] b = new byte[1024 * 10];
		while (true) {
			final int read = bufferedInputStream.read(b);
			if (read <= -1) {
				break;
			}
			bufferedOutputStream.write(b, 0, read);
		}

		bufferedOutputStream.flush();
		fileOutputStream.flush();

		bufferedOutputStream.close();
		fileOutputStream.close();

		bufferedInputStream.close();
		inputStream2.close();

	}

}
