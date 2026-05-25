package com.vo.zframework.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.vo.log.core.ZLog2;
import com.vo.zframework.anno.ZComponent;
import com.vo.zframework.cache.STU;
import com.vo.zframework.configuration.ServerConfigurationProperties;
import com.vo.zframework.configuration.TempDir;



/**
 * 本类早于API目标Method( @ZRequestMapping 标记的方法)之前执行，
 * 可以在API Method之前，在解析http请求时，执行自定义流程。
 *
 * 如： 上传文件form-data请求同时带一个header的验证码，
 * 	    如果把API Method声明为
 * 		(@ZRequestHeader(value = "gc") final String gc, final ZMultipartFile file)
 *  	然后在里面校验gc通过了再处理file，有可能出现gc错误甚至是恶意的脚本请求只为浪费服务器资源，
 *  	而此时的file参数已经读取解析并且存储完成，这个就白做了。
 *
 *  	所以可以自定义类 A extends 本类，A类加上 @ZComponent 然后覆盖本类中方法即可，
 *  	如上例子则覆盖 checkHeader 方法，使用 BodyReader.readHeader 得到 ZRequest然后
 *  	request.getHeader("gc")，在此校验不通过则抛异常，避免了后续的读取解析保存body部分的工作
 *
 *
 * 默认的http请求报文解析器，当前实现流程为：
 *
 * 1、读取请求行的METHOD
 * 2、读取header
 * 3、读取body(读入内存/form-data边读边写入临时文件等等)，
 * 		这一步根据前两步来判断是否读取，因为body是非必须的
 *
 * @author zhangzhen
 * @date 2024年12月22日 下午1:53:19
 *
 */
// FIXME 2024年12月22日 下午3:46:50 zhangzhen : 考虑好把这个类每个步骤都抽取出方法，供子类覆盖
// 再写一个类，调用这个方法来完成一个完整的解析http报文的流程。方法只给
// SocketChannel 或者ZArray等等，尽可能屏蔽内部实现，只让用户关注业务逻辑即可
@ZComponent
public class DefaultHttpReader {

	private static final  ZLog2 LOG = ZLog2.getInstance();

	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES = ZContext
			.getBean(ServerConfigurationProperties.class);

	public static void removeNB(final TF tf, final String boundary, final ZArray array) {

		final int bsC = 1024 * 100;
		final byte[] ba = new byte[bsC];
		boolean findBody = false;
		long tR = 0;
		int readCount = 0;
		int bodyStartI = -1;

		try (FileInputStream fis = new FileInputStream(tf.getFile());
				BufferedInputStream bis = new BufferedInputStream(fis)) {
			while (true) {
				final int read = bis.read(ba);
				if (read <= -1) {
					break;
				}
				readCount++;
				tR += read;

				final int ctI = BodyReader.search(ba, HeaderEnum.CONTENT_TYPE.getName(), 1, 0);
				if (ctI > -1) {
					final int crlf2I = BodyReader.search(ba, STU.CRLFCRLF, 1, ctI);
					if (crlf2I > ctI) {
						bodyStartI = crlf2I;

						// 找到了文件body开始位置了，直接删掉前面的
						findBody = true;

						final byte[] cc = Arrays.copyOfRange(ba, 0, bodyStartI);
						array.add(cc);
					}
				}

				if (!findBody) {
					throw new IllegalArgumentException("body没找到!!!!");
				}
				final int bendI = BodyReader.search(ba, "--" + boundary, 1, readCount == 1 ? bodyStartI : 0);
				if (bendI > -1) {
					final int bGGendI = BodyReader.search(ba, STU.CRLF + "--" + boundary, 1,
							readCount == 1 ? bodyStartI : 0);
					if (bGGendI < bendI) {

						final byte[] cc2 = Arrays.copyOfRange(ba, bGGendI, read);
						array.add(cc2);

						try (RandomAccessFile raf = new RandomAccessFile(tf.getFile().getAbsolutePath(), "rw")) {
							// 删除body后的CRLF的部分
							raf.setLength(tR - (read - bGGendI));
						}
						// 然后删除前面的开头的0到body开始的部分
						deleteFileBytes(tf.getFile().getAbsolutePath(), 0, bodyStartI + STU.CRLFCRLF.length());

						break;
					}
				}
			}

		} catch (final IOException e) {
			e.printStackTrace();
		}
	}


	public static void readFileNameAndContentType(final TF tf) {

		try (FileInputStream in = new FileInputStream(tf.getFile());
				final BufferedInputStream bufferedInputStream = new BufferedInputStream(in)) {

			final byte[] ba = new byte[1024 * 64];
			while (true) {
				final int read = bufferedInputStream.read(ba);
				if (read <= -1) {
					break;
				}

				final int ctI = BodyReader.search(ba, HeaderEnum.CONTENT_TYPE.getName(), 1, 0);
				if (ctI > -1) {
					final List<Integer> arrayList = new ArrayList<>();
					int i = 0;
					while (true) {
						final int sr = BodyReader.search(ba, HeaderEnum.CONTENT_DISPOSITION.getName(), i, 0);
						if (sr <= -1) {
							break;
						}
						arrayList.add(sr);
						i++;
					}

					int cdI = 0;
					// 找到小于ctI的最大的那个cdI
					for (int ix = arrayList.size(); ix-- > 0;) {
						if (arrayList.get(ix) < ctI) {
							cdI = arrayList.get(ix);
							break;
						}
					}

					final byte[] xxx = Arrays.copyOfRange(ba, cdI, ctI);
					final String cdLine = new String(xxx);
					final Map<String, String> cdMap = parseCDLine(cdLine);
					tf.setName(cdMap.get("name"));
					tf.setFileName(cdMap.get("filename"));
					final int crlf2I = BodyReader.search(ba, STU.CRLFCRLF, 1, ctI);
					if (crlf2I > ctI) {
						final byte[] ctBA = Arrays.copyOfRange(ba, ctI, crlf2I);
						final String contentType = gCT(new String(ctBA));
						tf.setContentType(contentType);
						break;
					}
				}
			}

		} catch (final IOException e) {
			e.printStackTrace();
		}
	}


	private static final String gCT(final String cts) {
		final int i = cts.indexOf(":");
		return cts.substring(i + 1).trim();
	}

	public static void closeTFStream(final TF tf) {
		if (tf == null) {
			return;
		}

		try {
			// buffer.size =1 就没问题，待会再看什么原因
			tf.getBufferedOutputStream().flush();
			tf.getOutputStream().flush();
			tf.getBufferedOutputStream().close();
			tf.getOutputStream().close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 问豆包要的方法：
	 *
	 * 通用方法：删除文件中指定范围的字节
	 *
	 * @param filePath 文件路径
	 * @param start    要删除的字节起始位置（从0开始计数）
	 * @param length   要删除的字节长度
	 * @return
	 * @throws IOException 文件操作异常
	 */
    public static void deleteFileBytes(final String filePath, final long start, final long length) throws IOException {


        // 1. 参数合法性校验
        if ((start < 0) || (length <= 0)) {
            throw new IllegalArgumentException("起始位置不能为负数，删除长度必须大于0");
        }

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            final long fileTotalLength = raf.length(); // 获取文件总长度
            // 校验删除范围是否超出文件边界
            if ((start + length) > fileTotalLength) {
                throw new IllegalArgumentException("删除范围超出文件总长度！文件总长度：" + fileTotalLength
                        + "，删除结束位置：" + (start + length));
            }

            // 2. 定义缓冲区（提升读写效率，可根据需求调整大小）
            final byte[] buffer = new byte[1024 * 64];
            int bytesRead; // 每次实际读取的字节数

            // 3. 定位到「要删除部分的下一个字节」（即需要向前移动的起始位置）
            long readPos = start + length;
            // 定位到「删除起始位置」（覆盖写入的目标位置）
            long writePos = start;

            // 4. 循环读取后续字节，并向前覆盖写入
            while (readPos < fileTotalLength) {
                raf.seek(readPos); // 移动读取指针到待读取位置
                bytesRead = raf.read(buffer); // 读取缓冲区大小的字节

                // 处理最后一次读取可能不足缓冲区大小的情况
                if (bytesRead == -1) {
                    break;
                }

                raf.seek(writePos); // 移动写入指针到目标位置
                raf.write(buffer, 0, bytesRead); // 写入读取到的字节

                // 更新指针位置
                readPos += bytesRead;
                writePos += bytesRead;
            }

            // 5. 截断文件：删除最后多余的字节（原长度 - 要删除的长度）
            raf.setLength(fileTotalLength - length);
        }

    }


	// FIXME 2024年12月20日 上午1:09:51 zhangzhen : 文件名重新考虑下
	public static TF saveToTempFile(final String fileNameRandom, final String name, final String fileName) {

		final String tempDirPath = mkdir();
		final String tempFilePath = tempDirPath + File.separator + fileNameRandom + ".temp";

		final File file = new File(tempFilePath);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		try {
			final FileOutputStream outputStream = new FileOutputStream(file);
			final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
			return new TF(file, tempFilePath, name, fileName, outputStream, bufferedOutputStream);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}

		return null;
	}

	private static String mkdir() {
		final String uploadTempDir = SERVER_CONFIGURATIONPROPERTIES.getUploadTempDir();
		if (STU.hasContent(uploadTempDir)) {
			final File dir = new File(uploadTempDir);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			return dir.getAbsolutePath();
		}

		final String userDir = TempDir.getUserDir();
		final File dir = new File(userDir + File.separator + "temp");
		if (!dir.exists()) {
			dir.mkdirs();
		}

		return dir.getAbsolutePath();
	}

	public static Map<String, String> parseCTLine(final String ctS) {


		return null;
	}
	public static Map<String, String> parseCDLine(final String cdLine) {

		return BodyReader.handleBodyContentDisposition(cdLine);

	}

	public static Fm hFM(final ZArray array) {
		final int boundaryStartIndex = BodyReader.search(array.get(), ZRequest.BOUNDARY, 1, 1);
		if (boundaryStartIndex <= -1) {
			return new Fm(false, "");
		}

		final int boundaryEndIndex = BodyReader.search(array.get(), STU.CRLF, 1,
				boundaryStartIndex + ZRequest.BOUNDARY.getBytes().length);
		if (boundaryEndIndex > boundaryStartIndex) {

			final byte[] copyOfRange = Arrays.copyOfRange(array.get(),
					boundaryStartIndex + ZRequest.BOUNDARY.getBytes().length, boundaryEndIndex);
			final String boundary = new String(copyOfRange);
			return new Fm(true, boundary);
		}

		return new Fm(false, "");
	}

}
