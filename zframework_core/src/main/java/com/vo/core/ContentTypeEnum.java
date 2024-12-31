package com.vo.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Content-Type
 *
 * @author zhangzhen
 * @date 2023年6月24日
 *
 */
@Getter
@AllArgsConstructor
public enum ContentTypeEnum {


	TEXT_PLAIN("Content-Type: text/plain;charset=UTF-8", "text/plain"),

	MULTIPART_FORM_DATA("Content-Type: multipart/form-data", "multipart/form-data"),

	URLENCODED("Content-Type: application/x-www-form-urlencoded", "application/x-www-form-urlencoded"),

	APPLICATION_OCTET_STREAM("Content-Type: application/octet-stream", "application/octet-stream"),

	APPLICATION_JSON("Content-Type: application/json;charset=UTF-8", "application/json"),

	APPLICATION_PDF("Content-Type: application/pdf;", "application/pdf"),

	APPLICATION_XML("Content-Type: application/xml;charset=UTF-8", "application/xml"),
	TEXT_HTML("Content-Type: text/html;charset=UTF-8", "text/html"),

	AUDIO_MP3("Content-Type: audio/mp3;", "audio/mp3"),

	TEXT_CSS("Content-Type: text/css;", "text/css"),

	IMAGE_GIF("Content-Type: image/gif;", "image/gif"),

	IMAGE_JPGE("Content-Type: image/jpeg;", "image/jpeg"),

	IMAGE_PNG("Content-Type: image/png;", "image/png"),

	VIDEO_MP4("Content-Type: video/mp4;", "video/mp4"),

	WORD("Content-Type: application/msword;", "application/msword"),
	IMAGE_JPG("Content-Type: image/jpg;", "image/jpg"),

	JS("Content-Type: application/javascript;", "application/javascript"),

	IMAGE_ICON("Content-Type: image/vnd.microsoft.icon;", "image/vnd.microsoft.ico"),

	GZIP("Content-Encoding: gzip", ""),

	;

	public static ContentTypeEnum gType(final String fileNameSuffix) {
		if (fileNameSuffix.endsWith("js")) {
			return JS;
		}
		if (fileNameSuffix.endsWith("doc") || fileNameSuffix.endsWith("docx")) {
			return ContentTypeEnum.WORD;
		}

		final ContentTypeEnum[] vs = values();
		for (final ContentTypeEnum ee : vs) {
			if (ee.getType().endsWith(fileNameSuffix)) {
				return ee;
			}

		}

		return null;
	}
	private String value;
	private String type;
}
