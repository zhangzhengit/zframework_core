package com.vo.core;

import com.vo.cache.J;

/**
 * Content-Type
 *
 * @author zhangzhen
 * @date 2023年6月24日
 *
 */
public enum ContentTypeEnum {


	TEXT_PLAIN("Content-Type: text/plain;charset=UTF-8", "text/plain"),

	MULTIPART_FORM_DATA("Content-Type: multipart/form-data", "multipart/form-data"),

	URLENCODED("Content-Type: application/x-www-form-urlencoded", "application/x-www-form-urlencoded"),

	APPLICATION_OCTET_STREAM("Content-Type: application/octet-stream", "application/octet-stream"),

	APPLICATION_JSON("Content-Type: application/json;charset=UTF-8", "application/json"){
		@Override
		public void body(final Object r, final ZResponse rx) {
			rx.body(J.toJSONString(r));
		}
	},

	APPLICATION_PDF("Content-Type: application/pdf;", "application/pdf"),

	APPLICATION_XML("Content-Type: application/xml;charset=UTF-8", "application/xml"){
		@Override
		public void body(final Object r, final ZResponse rx) {
			// FIXME 2025年12月6日 03:32:20 zhangzhen :  处理为xml
			super.body(r, rx);
		}
	},

	TEXT_HTML("Content-Type: text/html;charset=UTF-8", "text/html"),

	AUDIO_MP3("Content-Type: audio/mp3;", "audio/mp3"),

	AUDIO_WAV("Content-Type: audio/wav;", "audio/wav"),

	TEXT_CSS("Content-Type: text/css;", "text/css"),

	IMAGE_GIF("Content-Type: image/gif;", "image/gif"),

	IMAGE_JPGE("Content-Type: image/jpeg;", "image/jpeg"),

	IMAGE_PNG("Content-Type: image/png;", "image/png"),

	VIDEO_MP4("Content-Type: video/mp4;", "video/mp4"),

	FONT_TTF("Content-Type:  font/ttf;", "font/ttf"),

	FONT_WOFF("Content-Type:  font/ttf;", "font/woff"),

	WORD("Content-Type: application/msword;", "application/msword"),
	IMAGE_JPG("Content-Type: image/jpg;", "image/jpg"),

	JS("Content-Type: application/javascript;", "application/javascript"),

	IMAGE_ICON("Content-Type: image/vnd.microsoft.icon;", "image/vnd.microsoft.ico"),

	GZIP("Content-Encoding: gzip", ""),

	;

	public void body(final Object r,final ZResponse rx) {
		if (r instanceof byte[]) {
			rx.body((byte[]) r);
		} else if (r instanceof String) {
			rx.body((String) r);
		} else {
			rx.body(r);
		}
	}
	
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

	ContentTypeEnum(final String value, final String type) {
		this.value = value;
		this.type = type;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public String getType() {
		return this.type;
	}

	public void setType(final String type) {
		this.type = type;
	}

}

