package com.vo.configuration;

/**
 *
 *
 * @author zhangzhen
 * @date 2025年1月2日 下午10:16:15
 *
 */
public class TempDir {

	public static String getUserDir() {
		return System.getProperty("user.dir");
	}

	public static OSEnum getOS() {
		final String osName = System.getProperty("os.name").toLowerCase();

		if (osName.contains("win")) {
			return OSEnum.WINDOWS;
		}

		return OSEnum.LINUX;
	}

}
