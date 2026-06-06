package vo.zframework.core;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vo.zframework.cache.AU;
import vo.zframework.cache.STU;
import vo.zframework.configuration.ServerConfigurationProperties;
import vo.zframework.configuration.TempDir;
import vo.zframework.http.HttpStatusEnum;
import vo.zframework.validator.ZFException;

/**
 *
 * BodyReader
 *
 * @author zhangzhen
 * @date 2024年12月8日 下午12:24:28
 *
 */
public class HttpRequestParser {

	private static final byte[] CONTENT_DISPOSITION_BYTES = HeaderEnum.CONTENT_DISPOSITION.getName().getBytes();
	private static final byte[] CONTENT_TYPE_BYTES = HeaderEnum.CONTENT_TYPE.getName().getBytes();
	private static final byte[] BOUNDARY_BYTES = ZRequest.BOUNDARY.getBytes();
	public static final String BOUNDARY_PREFIX = "--";
	public static final String BOUNDARY_SUFFIX = "--";
	public static final String FILENAME = "filename";
	public static final String NAME = "name";

	public static final int CRLF_BYTES_LENGTH = STU.CRLF.getBytes().length;

	/**
	 * 从一个完整的http请求报文中解析出所有内容
	 *
	 * @param httpRequestBA 一个完整的http请求的 byte[]
	 * @return
	 */
	public static ZRequest parse(final byte[] httpRequestBA) {

		final int headerEndIndex = AU.search(httpRequestBA, STU.CRLFCRLF_BYTES, 1, 0);

		// FIXME 2026年6月7日 06:32:16 zhangzhen : 下面这个copy应该是不需要的，但是split 的是CRLF，而截止符号是CRLFCRLF，不好处理
		// 也不方便在split中处理

		final byte[] hba = Arrays.copyOfRange(httpRequestBA, 0, headerEndIndex);

		final List<String> lineList = STU.split(hba, STU.CRLF);
		final ZRequest request= new ZRequest(lineList);

		if ((headerEndIndex + STU.CRLFCRLF.length()) < httpRequestBA.length) {
			final byte[] bodyBA = Arrays.copyOfRange(httpRequestBA, headerEndIndex + STU.CRLFCRLF.length(), httpRequestBA.length);
			request.setBody(bodyBA);
		} else {
			request.setBody(new byte[] {});
		}

		return request;
	}

	/**
	 * 从file的formdata中解析出所有对象
	 *
	 * @param ba
	 * @param contentType
	 * @param boundary
	 * @return
	 */
	public static List<FormData> readFileFormData(final byte[] ba, final String contentType, final String boundary) {
		if (boundary == null) {
			return Collections.emptyList();
		}

		final int contentTypeIndex = AU.search(ba, contentType, 1, 0);

		if (contentTypeIndex <= -1) {
			return Collections.emptyList();
		}

		final List<FormData> fd2l = new ArrayList<>();

		final int bodySI = AU.search(ba, BOUNDARY_PREFIX + boundary, 1, 0);

		if (bodySI <= -1) {
			throw new ZFException("上传文件不存在", HttpStatusEnum.HTTP_400.getStatus());
		}

		final List<Integer> boundaryIndexList = new ArrayList<>();

		int i = 1;
		int fromIndex = 1;
		while (true) {
			final int iN = i;
			final int fromBAIndex = fromIndex;
			final int boundaryIndex = AU.search(ba, BOUNDARY_PREFIX + boundary, iN, fromBAIndex);
			if (boundaryIndex <= -1) {
				break;
			}
			i++;
			fromIndex = i + (BOUNDARY_PREFIX + boundary).length();
			boundaryIndexList.add(boundaryIndex);
		}

		for (int from = 0, to = 1; from < (boundaryIndexList.size() - 1); from++, to++) {
			final byte[] x = Arrays.copyOfRange(ba, boundaryIndexList.get(from), boundaryIndexList.get(to));
			final FormData one = handleOneItem(x);
			fd2l.add(one);
		}

		return fd2l;
	}

	/**
	 * 解析找到的两个boundary之间的内容byte[]为FormData
	 *
	 * @param ba
	 * @return
	 */
	public static FormData handleOneItem(final byte[] ba) {

		final FormData formData = new FormData();
		final int contentTypeIndex = AU.search(ba, CONTENT_TYPE_BYTES, 1, 0);
		if (contentTypeIndex > -1) {
			final int ctRNIndex = AU.search(ba, STU.CRLF_BYTES, 1, contentTypeIndex);
			if (ctRNIndex > -1) {
				final String ctX = new String(ba, contentTypeIndex, (ctRNIndex + STU.CRLF.length()) - contentTypeIndex).split(STU.COLON)[1].trim();
				formData.setContentType(ctX);

				final int bodyStartIndexX = AU.search(ba, STU.CRLFCRLF_BYTES, 1, 0);
				if (bodyStartIndexX > -1) {
					// XXX 注意：截止要减去一个CRLF的长度，因为参数byte[] 包含了body后面的一个空行
					final byte[] bodyBA = Arrays.copyOfRange(ba, bodyStartIndexX + STU.CRLFCRLF.length(),
							ba.length - STU.CRLF.length());
					formData.setBody(bodyBA);
				}

			}
		} else {
			final int bodyStartIndexX = AU.search(ba, STU.CRLFCRLF_BYTES, 1, 0);
			if (bodyStartIndexX > -1) {
				final String value = new String(ba, bodyStartIndexX + STU.CRLFCRLF.length(),
						ba.length - (bodyStartIndexX + STU.CRLFCRLF.length()));
				formData.setValue(value);
			}
		}

		final int cdIndex = AU.search(ba, CONTENT_DISPOSITION_BYTES, 1, 0);

		if (cdIndex > -1) {
			final int cdRNIndex = AU.search(ba, STU.CRLF_BYTES, 1, cdIndex);
			if (cdRNIndex > -1) {
				final String line = new String(ba, cdIndex, (cdRNIndex + STU.CRLF.length()) - cdIndex);
				final Map<String, String> vMap = handleBodyContentDisposition(line);
				formData.setName(vMap.get(NAME));
				formData.setFileName(vMap.get(FILENAME));
			}
		}

		return formData;
	}

	/**
	 * 把body中的Content-Disposition这行的k=v的形式存为一个Map
	 *
	 * @param contentDisposition
	 * 			具体的行，如：Content-Disposition: form-data; name="file"; filename="2.jpg"
	 *
	 * @return
	 */
	public static Map<String, String> handleBodyContentDisposition(final String contentDisposition) {
		final Map<String, String> vMap = new HashMap<>(4, 1F);
		final String[] a = contentDisposition.split(STU.SEMICOLON);
		for (final String a1 : a) {
			if (a1.contains(STU.EQUALS)) {
				final String[] a2 = a1.split(STU.EQUALS);
				vMap.put(a2[0].trim(), a2[1].trim().replace("\"", ""));
			}
		}
		return vMap;
	}


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

				final int ctI = AU.search(ba, CONTENT_TYPE_BYTES, 1, 0);
				if (ctI > -1) {
					final int crlf2I = AU.search(ba, STU.CRLFCRLF_BYTES, 1, ctI);
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
				final int bendI = AU.search(ba, "--" + boundary, 1, readCount == 1 ? bodyStartI : 0);
				if (bendI > -1) {
					final int bGGendI = AU.search(ba, STU.CRLF + "--" + boundary, 1, readCount == 1 ? bodyStartI : 0);
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

				final int ctI = AU.search(ba, CONTENT_TYPE_BYTES, 1, 0);
//				final int ctI = AU.search(ba, HeaderEnum.CONTENT_TYPE.getName(), 1, 0);
				if (ctI > -1) {
					final List<Integer> arrayList = new ArrayList<>();
					int i = 0;
					while (true) {
						final int iN = i;
						final int sr = AU.search(ba, CONTENT_DISPOSITION_BYTES, iN, 0);
//						final int sr = AU.search(ba, HeaderEnum.CONTENT_DISPOSITION.getName(), iN, 0);
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

					final String cdLine = new String(ba, cdI, ctI - cdI);
					final Map<String, String> cdMap = parseCDLine(cdLine);
					tf.setName(cdMap.get("name"));
					tf.setFileName(cdMap.get("filename"));
					final int crlf2I = AU.search(ba, STU.CRLFCRLF_BYTES, 1, ctI);
					if (crlf2I > ctI) {
						final String contentType = gCT(new String(ba, ctI, crlf2I - ctI));
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

		return HttpRequestParser.handleBodyContentDisposition(cdLine);

	}

	public static Fm hFM(final ZArray array) {
		final int boundaryStartIndex = AU.search(array.getRawArray(), BOUNDARY_BYTES, 1, 1);
		if (boundaryStartIndex <= -1) {
			return new Fm(false, "");
		}

		final int boundaryEndIndex = AU.search(array.getRawArray(), STU.CRLF_BYTES, 1, boundaryStartIndex + BOUNDARY_BYTES.length);
		if (boundaryEndIndex > boundaryStartIndex) {

			final String boundary = new String(array.getRawArray(),
					boundaryStartIndex + BOUNDARY_BYTES.length,
					boundaryEndIndex - (boundaryStartIndex + BOUNDARY_BYTES.length));
			return new Fm(true, boundary);
		}

		return new Fm(false, "");
	}

}
