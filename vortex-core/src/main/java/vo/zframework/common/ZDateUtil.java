package vo.zframework.common;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Date相关
 *
 * @author zhangzhen
 * @date 2024年12月6日 下午11:43:27
 *
 */
public class ZDateUtil {

	private static final int _1000 = 1000;
	private static final AtomicLong LAST_SECOND = new AtomicLong(0);
	private static final AtomicLong LAST_SECOND_BYTES = new AtomicLong(0);
	private static volatile String CACHED_DATE_STRING = "";
	private static volatile byte[] CACHED_DATE_STRING_BYTES = "".getBytes();

	private final static DateTimeFormatter FORMATTER = DateTimeFormatter
			.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH).withZone(ZoneOffset.UTC);

	public static String gmt(final Date date) {
		final ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
		return FORMATTER.format(zonedDateTime);
	}

	public static long toTimestampMillis(final String dateStr) {
		final ZonedDateTime zdt = ZonedDateTime.parse(dateStr, FORMATTER);
		return zdt.toInstant().toEpochMilli();
	}

	public static byte[] getCurrentGmtDateBytes() {
		final long nowSeconds = System.currentTimeMillis() / _1000;
		if (nowSeconds != LAST_SECOND_BYTES.get()) {
			synchronized (ZDateUtil.class) {
				if (nowSeconds != LAST_SECOND_BYTES.get()) {
					CACHED_DATE_STRING_BYTES = FORMATTER.format(ZonedDateTime.now(ZoneOffset.UTC)).getBytes();
					LAST_SECOND_BYTES.set(nowSeconds);
				}
			}
		}
		return CACHED_DATE_STRING_BYTES;
	}

	public static String getCurrentGmtDate() {
		final long nowSeconds = System.currentTimeMillis() / _1000;
		if (nowSeconds != LAST_SECOND.get()) {
			synchronized (ZDateUtil.class) {
				if (nowSeconds != LAST_SECOND.get()) {
					CACHED_DATE_STRING = FORMATTER.format(ZonedDateTime.now(ZoneOffset.UTC));
					LAST_SECOND.set(nowSeconds);
				}
			}
		}
		return CACHED_DATE_STRING;
	}

}
