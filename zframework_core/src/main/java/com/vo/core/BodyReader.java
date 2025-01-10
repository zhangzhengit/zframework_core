package com.vo.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

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
	public static final int RN_BYTES_LENGTH = RN.getBytes().length;

	/**
	 * 从http请求报文中解析出header，是只解析header，不解析header下面的部分
	 *
	 * @param ba 完整的http请求报文
	 * @return
	 */
	public static ZRequest readHeader(final byte[] ba) {
		final int headerEndIndex = search(ba, RNRN, 1, 0);

		final byte[] headerBA = Arrays.copyOfRange(ba, 0, headerEndIndex);
		final String[] headerKVString = new String(headerBA).split(RN);





		final ZRequest request= new ZRequest(headerKVString);

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

		// 执行到此，headerEndIndex < ba.length 则说明header后面还有内容，此内容就是body
		if (headerEndIndex < ba.length) {
			final byte[] copyOfRange = Arrays.copyOfRange(ba, headerEndIndex + RNRN.getBytes().length, ba.length);
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
	public static List<FD2> readFormDate(final byte[] ba, final String contentType, final String boundary) {

		// final int hash = ba.hashCode();
		// final int hash = Arrays.hashCode(ba);
		final String k = "ba" + "-" + ba.length + "-" + boundary.hashCode();
		final List<FD2> v1 = c.get(k);
		if (v1 != null) {
			return v1;
		}

		synchronized (k.intern()) {
			final List<FD2> v2 = c.get(k);
			if (v2 != null) {
				return v2;
			}

			final List<FD2> v = readFormDate0(ba, contentType, boundary);
			c.put(k, v);
			final long t2 = System.currentTimeMillis();
			return v;
		}

	}

	static Map<String, List<FD2>> c = new WeakHashMap<>(16, 1F);

	private static List<FD2> readFormDate0(final byte[] ba, final String contentType, final String boundary) {
		if (boundary == null) {
			return Collections.emptyList();
		}

		final int contentTypeIndex = search(ba, contentType, 1, 0);

		if (contentTypeIndex <= -1) {
			return Collections.emptyList();
		}

		int bI = 1;
		final List<Integer> biList = new ArrayList<>(4);
		while (true) {
			final int boundaryIndex = search(ba, RN + BOUNDARY_PREFIX + boundary, bI,
					contentTypeIndex + contentType.getBytes().length);
			if (boundaryIndex <= -1) {
				break;
			}
			biList.add(boundaryIndex);
			bI++;
		}

		final List<FD2> fd2l = new ArrayList<>();

		for (int from = 0; from < (biList.size() - 1); from++) {
			final int to = from + 1;
			final byte[] oneBA = Arrays.copyOfRange(ba,
					biList.get(from) + (RN + BOUNDARY_PREFIX + boundary).getBytes().length + RN.getBytes().length,
					biList.get(to));
			final FD2 one = handleOneItem(oneBA);
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

		final byte[] ba = oneBA;
		final List<Byte> bl = new ArrayList<>();
		// FIXME 2024年12月17日 下午4:05:16 zhangzhen : 这个方法要改进，不要for循环了，直接search CD CT RNRN等等
		final int ctIndex = search(oneBA, HeaderEnum.CONTENT_TYPE.getName(), 1, 0);
		for (int i = 0; i < ba.length; i++) {
			if (ba[i] == '\r') {
				if ((i < (ba.length - 1)) && (ba[i + 1] == '\n')) {
					final byte[] lineBA = listToArray(bl);
					final String line = new String(lineBA);

					if (line.startsWith(HeaderEnum.CONTENT_DISPOSITION.getName())) {
						fd2.setContentDisposition(line);
						final Map<String, String> vMap = handleBodyContentDisposition(line);
						fd2.setName(vMap.get(NAME));
						fd2.setFileName(vMap.get(FILENAME));
					}
					if (ctIndex > -1) {
						if (line.startsWith(HeaderEnum.CONTENT_TYPE.getName())) {
							final String[] ctA = line.split(":");
							final String ct = ctA[1].trim();
							fd2.setContentType(ct);

							final byte[] bodyFullBA = Arrays.copyOfRange(oneBA,
									ctIndex + line.getBytes().length + RNRN.getBytes().length, oneBA.length);
							fd2.setBody(bodyFullBA);
							break;
						}
					} else {
						final int bodyStartIndex = search(oneBA, RNRN, 1, i);
						final byte[] bodyV = Arrays.copyOfRange(ba, bodyStartIndex + RNRN.getBytes().length, ba.length);
						final String bV = new String(bodyV);
						fd2.setValue(bV);
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
	public static Map<String, String> handleBodyContentDisposition(final String line) {
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

			// 当前字节的上面是\r
			if (find && (i > 0) && (ba[i - 1] == '=')) {
				find = false;
				break;
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
