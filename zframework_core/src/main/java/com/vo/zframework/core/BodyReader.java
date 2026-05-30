package com.vo.zframework.core;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vo.zframework.cache.STU;
import com.vo.zframework.http.HttpStatusEnum;
import com.vo.zframework.validator.ZFException;

/**
 *
 * BodyReader
 *
 * @author zhangzhen
 * @date 2024年12月8日 下午12:24:28
 *
 */
public class BodyReader {

	public static final String BOUNDARY_PREFIX = "--";
	public static final String BOUNDARY_SUFFIX = "--";
	public static final String FILENAME = "filename";
	public static final String NAME = "name";
	public static final int RN_BYTES_LENGTH = STU.CRLF.getBytes().length;

	/**
	 * 从一个完整的http请求报文中解析出所有内容
	 * @param socket TODO
	 * @param ba
	 *
	 * @return
	 */
	public static ZRequest parse(final byte[] fullBA, final Socket socket) {

		final int headerEndIndex = search(fullBA, STU.CRLFCRLF, 1, 0);

		final byte[] headerBA = Arrays.copyOfRange(fullBA, 0, headerEndIndex);
		final String[] headerKVString = new String(headerBA).split(STU.CRLF);

		final ZRequest request= new ZRequest(headerKVString, socket);

		if ((headerEndIndex + STU.CRLFCRLF.length()) < fullBA.length) {
			final byte[] bodyBA = Arrays.copyOfRange(fullBA, headerEndIndex + STU.CRLFCRLF.length(), fullBA.length);
			request.setBody(bodyBA);
		} else {
			request.setBody(new byte[] {});
		}

		return request;
	}

	/**
	 * 从http请求报文中解析出header，是只解析header，不解析header下面的部分
	 *
	 * @param ba
	 * @return
	 */
	public static ZRequest parseHeader(final byte[] ba) {

		final int headerEndIndex = search(ba, STU.CRLFCRLF, 1, 0);

		final byte[] headerBA = Arrays.copyOfRange(ba, 0, headerEndIndex);
		final String[] headerKVString = new String(headerBA).split(STU.CRLF);

		final ZRequest request= new ZRequest(headerKVString, null);

		final byte[] readFullBody = readFullBody(ba, request.getContentType(), headerEndIndex, request.getBoundary());
		request.setBody(readFullBody);

		return request;
	}

	/**
	 * 从完整的http请求报文中解析出完整的body部分，返回body部分的byte[]
	 *
	 * @param ba
	 * @param contentType    header中的 Content-Type
	 * @param headerEndIndex header截止符号(\r\n\r\n)在ba中的位置
	 * @param boundary       header中的 Content-Type中的boundary值，有则传，无则传null
	 * @return
	 */
	public static byte[] readFullBody(final byte[] ba, final String contentType, final int headerEndIndex, final String boundary) {

		final int contentTypeIndex = search(ba, contentType, 1, 0);

		if (contentTypeIndex <= -1) {
			return null;
		}

		// boundary 不为空表示formdata，则根据 boundary来截取body
		if (boundary != null) {
			final int boundaryStartIndex = search(ba, boundary, 1, contentTypeIndex);
			if (boundaryStartIndex > -1) {
				final int boundaryEndIndex = search(ba, STU.CRLF + BOUNDARY_PREFIX + boundary + BOUNDARY_SUFFIX, 1, boundaryStartIndex);
				if (boundaryEndIndex > boundaryStartIndex) {
					final byte[] fullBodyBA = Arrays.copyOfRange(ba,
							boundaryStartIndex + boundary.getBytes().length + STU.CRLF.getBytes().length,
							boundaryEndIndex);
					return fullBodyBA;
				}
			}
		}

		// 执行到此，headerEndIndex < ba.length 则说明header后面还有内容，此内容就是body
		if (headerEndIndex < ba.length) {
			final byte[] copyOfRange = Arrays.copyOfRange(ba, headerEndIndex + STU.CRLFCRLF.getBytes().length, ba.length);
			return copyOfRange;
		}

		return null;
	}

	/**
	 * 从 formdata的http请求报文中解析出所有对象
	 *
	 * @param ba
	 * @param contentType
	 * @param boundary
	 * @return
	 */
	public static List<FD2> readFormData(final byte[] ba, final String contentType, final String boundary) {
		return readFormData0(ba, contentType, boundary);
	}

	private static List<FD2> readFormData0(final byte[] ba, final String contentType, final String boundary) {
		if (boundary == null) {
			return Collections.emptyList();
		}

		final int contentTypeIndex = search(ba, contentType, 1, 0);

		if (contentTypeIndex <= -1) {
			return Collections.emptyList();
		}

		final List<FD2> fd2l = new ArrayList<>();

		final int bodySI = search(ba, BOUNDARY_PREFIX + boundary, 1,0);

		if (bodySI <= -1) {
			throw new ZFException("上传文件不存在", HttpStatusEnum.HTTP_400.getCode());
		}

		final List<Integer> r = new ArrayList<>();

		int i = 1;
		int fromIndex = 1;
		while (true) {
			final int search = BodyReader.search(ba, BOUNDARY_PREFIX + boundary, i, fromIndex);
			if (search <= -1) {
				break;
			}
			i++;
			fromIndex = i + (BOUNDARY_PREFIX + boundary).length();
			r.add(search);
		}

		for (int from = 0, to = 1; from < (r.size() - 1); from++, to++) {
			final byte[] x = Arrays.copyOfRange(ba, r.get(from),  r.get(to));
			final FD2 one = handleOneItem(x);
			fd2l.add(one);
		}

		return fd2l;
	}

	/**
	 * 处理找到的一个boundary之间的内容byte[]
	 *
	 * @param oneBA
	 * @return
	 */
	public static FD2 handleOneItem(final byte[] oneBA) {

		final FD2 fd2 = new FD2();
		final int ctIndex = search(oneBA, HeaderEnum.CONTENT_TYPE.getName(), 1, 0);
		if (ctIndex > -1) {
			final int ctRNIndex = search(oneBA, STU.CRLF, 1, ctIndex);
			if (ctRNIndex > -1) {
				final byte[] ctBA = Arrays.copyOfRange(oneBA, ctIndex, ctRNIndex + STU.CRLF.length());
				final String ctX = new String(ctBA).split(STU.COLON)[1].trim();
				fd2.setContentType(ctX);

				final int bodyStartIndexX = search(oneBA, STU.CRLFCRLF, 1, 0);
				if (bodyStartIndexX > -1) {
					// XXX 注意：截止要减去一个CRLF的长度，因为参数byte[] 包含了body后面的一个空行
					final byte[] bodyBA = Arrays.copyOfRange(oneBA, bodyStartIndexX + STU.CRLFCRLF.length(),
							oneBA.length - STU.CRLF.length());
					fd2.setBody(bodyBA);
				}

			}
		} else {
			final int bodyStartIndexX = search(oneBA, STU.CRLFCRLF, 1, 0);
			if (bodyStartIndexX > -1) {
				final byte[] valueBA = Arrays.copyOfRange(oneBA, bodyStartIndexX + STU.CRLFCRLF.length(), oneBA.length);
				final String value = new String(valueBA);
				fd2.setValue(value);
			}
		}

		final int cdIndex = search(oneBA, HeaderEnum.CONTENT_DISPOSITION.getName(), 1, 0);

		if (cdIndex > -1) {
			final int cdRNIndex = search(oneBA, STU.CRLF, 1, cdIndex);
			if (cdRNIndex > -1) {
				final byte[] cdBA = Arrays.copyOfRange(oneBA, cdIndex, cdRNIndex + STU.CRLF.length());
				final String line = new String(cdBA);
				final Map<String, String> vMap = handleBodyContentDisposition(line);
				fd2.setName(vMap.get(NAME));
				fd2.setFileName(vMap.get(FILENAME));
			}
		}

		return fd2;
	}

	/**
	 * 把body中的Content-Disposition这行的k=v的形式存为一个Map
	 *
	 * @param line
	 * @return
	 */
	public static Map<String, String> handleBodyContentDisposition(final String line) {
		final Map<String, String> vMap = new HashMap<>(4, 1F);
		final String[] a = line.split(STU.SEMICOLON);
		for (final String a1 : a) {
			if (a1.contains(STU.EQUALS)) {
				final String[] a2 = a1.split(STU.EQUALS);
				vMap.put(a2[0].trim(), a2[1].trim().replace("\"", ""));
			}
		}
		return vMap;
	}

	public static byte[] listToArray(final List<Byte> bl) {
		final byte[] b = new byte[bl.size()];
		for (int i = 0; i < bl.size(); i++) {
			b[i] = bl.get(i);
		}

		return b;
	}

	/**
	 * 按行搜索关键字出现在byte[]中的位置
	 *
	 * @param ba          http完整的请求内容数组
	 * @param keyword     关键字
	 * @param iN          第几次出现的位置
	 * @param fromBAIndex 从ba数组开始搜索的位置
	 * @return
	 */
	public static int search(final byte[] ba,final String keyword, final int iN, final int fromBAIndex) {
		if ((keyword == null) || keyword.isEmpty()) {
			return -1;
		}
		final byte[] kb = keyword.getBytes();

		int findN = 0;
		for (int i = fromBAIndex; i < ba.length; i++) {
			boolean find = true;
			if (i >= ((ba.length - kb.length) + 1)) {
				find = false;
				break;
			}
			for (int k = 0; k < kb.length; k++) {
				if (ba[i + k] != kb[k]) {
					find = false;
					break;
				}
			}

			// FIXME 2025年12月4日 下午8:23:49 zhangzhen: 下面if先注释，因为发现了bug了，不知道当时为什么这么写了
			// 当前字节的上面是\r
//			if (find && (i > 0) && (ba[i - 1] == '=')) {
//				find = false;
//				break;
//			}
			if (find) {
				findN++;
				if (findN >= iN) {
					return i;
				}
			}
		}

		return -1;
	}

}
