package vo.zframework.http.request;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 接收 multipart/form-data 上传的文件，专指文件。
 * 用在接口方法上声明为一个参数即可，如：
 *
    @ZRequestMapping(mapping = { "/upload" }, method = MethodEnum.POST)
	public CR upload(final ZMultipartFile file){
		// xxx
	}

	即可接收到上传的文件

 *
 * @author zhangzhen
 * @date 2023年10月26日
 *
 */
public class ZMultipartFile {

	private static final int BUFFER_SIZE = 1024 * 10;

	private final String name;
	private final String tempFilePath;
	private final String originalFilenameString;
	private final byte[] content;
	private final long fileSize;
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
		return this.fileSize;
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
	 * 返回文件的的全部byte[],比 @see getContent 方法多了同时返回临时文件中的全部byte[]
	 * 本方法用于不管是读入内存还是写入临时文件都用一次性读入文件全部内容的情况
	 *
	 * @return
	 */
	public byte[] toByteArray() {
		if (!this.isTempFile()) {
			return this.getContent();
		}
		final byte[] buffer =new byte[1028 * 8];
		// FIXME 2026年5月26日 19:00:31 zhangzhen : 这个还不好写、数组容量受int.maxvalue限制，
		// 还不能限制上传文件最大2GB，考虑好怎么写

		return null;
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

	public String getName() {
		return this.name;
	}

	public String getTempFilePath() {
		return this.tempFilePath;
	}

	public String getOriginalFilenameString() {
		return this.originalFilenameString;
	}

	public ZMultipartFile(final String name, final String tempFilePath, final String originalFilenameString, final byte[] content,
			final boolean isTempFile, final String contentType, final InputStream inputStream, final long fileSize) {
		this.name = name;
		this.tempFilePath = tempFilePath;
		this.originalFilenameString = originalFilenameString;
		this.content = content;
		this.fileSize = fileSize;
		this.isTempFile = isTempFile;
		this.contentType = contentType;
		this.inputStream = inputStream;
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

		try (InputStream inputStream2 = this.getInputStream();
				BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream2);
				FileOutputStream fileOutputStream = new FileOutputStream(dest);
				BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {
			final byte[] buffer = new byte[BUFFER_SIZE];
			while (true) {
				final int read = bufferedInputStream.read(buffer);
				if (read <= -1) {
					break;
				}
				bufferedOutputStream.write(buffer, 0, read);
			}

			bufferedOutputStream.flush();
			fileOutputStream.flush();
		}

	}

	public long getFileSize() {
		return this.fileSize;
	}

}
