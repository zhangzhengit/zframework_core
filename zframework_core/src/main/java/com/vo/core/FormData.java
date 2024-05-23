package com.vo.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年10月23日
 *
 */
@Data
public class FormData {

//	[Content-Disposition: form-data; name="img"; filename="anuo.jpg", Content-Type: image/jpeg, , xxx]
//	[Content-Disposition: form-data; name="name", , zhangsan]
//	[Content-Disposition: form-data; name="id", , 200]

	public static final String FILE_NAME = "filename";
	public static final String NAME = "name";
	public static final String CONTENT_TYPE= "Content-Type";
	public static final String CONTENT_DISPOSITION_FORM_DATA = "Content-Disposition: form-data";

	private String[] origin;

	public FormData(final String[] origin) {
		this.origin = origin;
	}

	public String getValue() {

		final int from = StrUtil.isEmpty(this.getContentType()) ? 2 : 3;

		final String[] ss = Arrays.copyOfRange(this.origin, from, this.origin.length);
		final StringBuilder builder = new StringBuilder();
		for (int i = 0; i < ss.length; i++) {
			final String x = ss[i];
			builder.append(x.trim());
			if (i < (ss.length - 1)) {
				builder.append(Task.NEW_LINE);
			}
		}

		return builder.toString();
	}

	public String getFileName() {
		for (final String x : this.origin) {
			if (x.startsWith(CONTENT_DISPOSITION_FORM_DATA)) {
				final String[] aa = x.split(";");
				for (final String a : aa) {
					final String[] bb = a.split("=");
					if (FILE_NAME.equals(bb[0].trim())) {
						return bb[1].trim().replaceAll("\"", "");
					}
				}
			}
		}

		return null;
	}

	public String getName() {
		for (final String x : this.origin) {
			if (x.startsWith(CONTENT_DISPOSITION_FORM_DATA)) {
				final String[] aa = x.split(";");
				for (final String a : aa) {
					final String[] bb = a.split("=");
					if (NAME.equals(bb[0].trim())) {
						return bb[1].trim().replaceAll("\"", "");
					}
				}
			}
		}

		return null;
	}

	public String getContentType() {
		for (final String string : this.origin) {
			if (string.startsWith(CONTENT_TYPE)) {
				final String[] a = string.split(":");
				return a[1].trim();
			}
		}

		return null;
	}

	public boolean isFormData() {

		for (final String string : this.origin) {
			if (string.startsWith(CONTENT_DISPOSITION_FORM_DATA)) {
				return true;
			}
		}

		return false;
	}

	public static List<FormData> parseFormData(final byte[] ba) {

		final String requestString = new String(ba, NioLongConnectionServer.CHARSET);
		final String[] sp = requestString.split(Task.NEW_LINE);

		final String boundary = "--" +  getBoundary(requestString, sp);
		final List<FormData> parseFormData = parseFormData(sp, boundary);

		return parseFormData;
	}


	/**
	 * 从form-data的body中解析出 K、V、filename等
	 *
	 * @param sp       http请求的body
	 * @param boundary 分隔符，如：--------------------------106633430979185032152143
	 * @return
	 *
	 */
	public static List<FormData> parseFormData(final String[] sp, final String boundary) {

		final ArrayList<FormData> r = Lists.newArrayList();

		final String k =  boundary;

		int start = 0;
		int end = 0;
		for (int i = 0; i < sp.length; i++) {

			final String x = sp[i];
			if (!k.equals(x)) {
				continue;
			}

			start = i;
			while ((start < sp.length) && (i < sp.length)) {
				i++;
				end = i;
				if ((i >= sp.length) || sp[end].startsWith(k)) {

					final String[] one = Arrays.copyOfRange(sp, start + 1, end);
//					System.out.println("one.length = " + one.length);
//					System.out.println("one = " + Arrays.toString(one));

					final FormData fff = new FormData(one);
					final String contentType = fff.getContentType();
//					System.out.println("contentType =  " + contentType);

					final String name = fff.getName();
//					System.out.println("name =  " + name);

					final String value = fff.getValue();
//					System.out.println("value.length =  " + value.length());

					final String fileName = fff.getFileName();
//					System.out.println("fileName =  " + fileName);

					final boolean formData = fff.isFormData();
//					System.out.println("formData =  " + formData);

					r.add(fff);

//					System.out.println();

					i--;
					break;
				}
			}
		}

		return r;
	}

	private static String getBoundary(final String requestString, final String[] sp) {
		final int i = requestString.indexOf("Content-Type: multipart/form-data;");
		if (i > -1) {
			for (final String x : sp) {
				if (x.indexOf("Content-Type: multipart/form-data;") > -1) {
					final int bi = x.indexOf("boundary=");
					if (bi > -1) {
						final String boundary = x.substring(bi + "boundary=".length());
//						System.out.println("boundary = " + boundary);

						return boundary;
					}
				}
			}

		}

		return null;
	}
}
