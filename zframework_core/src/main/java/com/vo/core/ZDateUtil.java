package com.vo.core;

import java.text.SimpleDateFormat;
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

	private final static SimpleDateFormat SDF = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'",Locale.ENGLISH);

	public static String gmt(final Date date) {

		SDF.setTimeZone(TimeZone.getTimeZone("GMT"));

		return SDF.format(date);

	}

}
