package com.vo.core;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 *
 *
 * @author zhangzhen
 * @date 2024年12月6日 下午11:43:27
 *
 */
public class ZDateUtil {

	private final static DateTimeFormatter FORMATTER = DateTimeFormatter
			.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH).withZone(ZoneOffset.UTC); // 确保使用 UTC 时区


	public static String gmt(final Date date) {
		final ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
		return FORMATTER.format(zonedDateTime);
	}

}
