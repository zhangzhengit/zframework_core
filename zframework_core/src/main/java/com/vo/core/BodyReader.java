package com.vo.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	public static final String RNRN = "\r\n\r\n";
	public static final String RN = "\r\n";

	public static ZRequest readHeader(final byte[] ba) {
		final int headerEndIndex = search(ba, RNRN, 1, 0);

		final byte[] hba = Arrays.copyOfRange(ba, 0, headerEndIndex);
		final String h = new String(hba);
		final String[] a = h.split(RN);

		final ZRequest request = Task.handleRead(a);

		final byte[] readFullBody = readFullBody(request, ba, headerEndIndex);
		request.setBody(readFullBody);

		return request;
	}

	public static byte[] readFullBody(final ZRequest request, final byte[] ba, final int headerEndIndex) {

		final String contentType = request.getContentType();
		final int contentTypeIndex = search(ba, contentType, 1, 0);

		if (contentTypeIndex <= -1) {
			return null;
		}

		// boundary 不为空表示formdata，则根据 boundary来截取body
		final String boundary = request.getBoundary();
		if (boundary != null) {
			final int boundaryStartIndex = search(ba, boundary, 1, contentTypeIndex);
			if ((boundaryStartIndex > -1)) {
				final int boundaryEndIndex = search(ba, RN + BOUNDARY_PREFIX + boundary + BOUNDARY_SUFFIX, 1, boundaryStartIndex);
				if (boundaryEndIndex > boundaryStartIndex) {
					final byte[] fullBodyBA = Arrays.copyOfRange(ba,
							boundaryStartIndex + boundary.getBytes().length + RN.getBytes().length,
							boundaryEndIndex);
					return fullBodyBA;
				}
			}
		}

		// headerEndIndex < ba.length 则说明header后面还有内容，此内容就是body
		if (headerEndIndex < ba.length) {
			final byte[] copyOfRange = Arrays.copyOfRange(ba, headerEndIndex + RNRN.getBytes().length, ba.length);
			return copyOfRange;
		}

		return null;
	}

	public static List<FD2> readBody(final ZRequest request, final byte[] ba) {

		final String boundary = request.getBoundary();
		if (boundary == null) {
			return Collections.emptyList();
		}

		final String contentType = request.getContentType();


		final int contentTypeIndex = search(ba, contentType, 1, 0);

		if (contentTypeIndex <= -1) {
			return Collections.emptyList();
		}

		final int boundaryStartIndex = search(ba, boundary, 1, contentTypeIndex + contentType.getBytes().length);

		final List<FD2> fd2l = new ArrayList<>();
		// FIXME 2024年12月8日 下午2:57:14 zhangzhen : 继续循环解析，现在只支持仅有一个文件的
		if ((boundaryStartIndex > -1)) {
			final int boundaryStartIndex2 = search(ba, RN + BOUNDARY_PREFIX + boundary, 1, boundaryStartIndex);
			if((boundaryStartIndex2 > boundaryStartIndex)) {

				final byte[] bodyBA = Arrays.copyOfRange(ba,
						boundaryStartIndex + boundary.getBytes().length + RN.getBytes().length, boundaryStartIndex2);
				final FD2 fd2 = handleOneItem(bodyBA);
				fd2l.add(fd2);
			}

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

		final byte[] ba = oneBA;
		final List<Byte> bl = new ArrayList<>();
		boolean findCT = false;
		String ctLIne = null;
		for (int i = 0; i < ba.length; i++) {
			if (ba[i] == '\r') {
				if ((i < (ba.length - 1)) && (ba[i + 1] == '\n')) {
					final byte[] lineBA = listToArray(bl);
					final String line = new String(lineBA);

					if (line.startsWith(FormData.CONTENT_DISPOSITION)) {
						fd2.setContentDisposition(line);
						final Map<String, String> vMap = handleBodyContentDisposition(line);
						fd2.setName(vMap.get(NAME));
						fd2.setFileName(vMap.get(FILENAME));
					}

					final int ctIndex = search(oneBA, FormData.CONTENT_TYPE, 1, 0);
					if (ctIndex > -1) {
						findCT = true;
					}

					if (findCT && line.startsWith(FormData.CONTENT_TYPE)) {
						final String[] ctA = line.split(":");
						final String ct = ctA[1].trim();
						ctLIne = line;
						fd2.setContentType(ct);
					}

					if (ctLIne != null) {
						final byte[] bodyFullBA = Arrays.copyOfRange(oneBA, ctIndex +
								+ ctLIne.getBytes().length
								+ RNRN.getBytes().length,
								oneBA.length);
						fd2.setBody(bodyFullBA);

						break;
					}

				}

				bl.clear();
			} else {
				if (ba[i] != '\n') {
					bl.add(ba[i]);
				}
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
	private static Map<String, String> handleBodyContentDisposition(final String line) {
		final Map<String, String> vMap = new HashMap<>();
		final String[] a = line.split(";");
		for (final String a1 : a) {
			if (a1.contains("=")) {
				final String[] a2 = a1.split("=");
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
			if (i > (ba.length - kb.length)) {
				find = false;
			}
			for (int k = 0; k < kb.length; k++) {
				if (ba[i + k] != kb[k]) {
					find = false;
					break;
				}
			}

			// 当前字节的上面是\r
			if (find && (ba[i - 1] == '=')) {
				find = false;
			}
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
