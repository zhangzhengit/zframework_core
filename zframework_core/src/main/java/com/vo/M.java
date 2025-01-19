package com.vo;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 *
 * @author zhangzhen
 * @data 2024年5月23日 下午7:10:19
 *
 */
public class M {

	public static void main(final String[] args) {

	}

	public static String getAppName() {
		final String projectPath = System.getProperty("user.dir");
		final String projectName = projectPath.substring(projectPath.lastIndexOf(File.separator) + 1);
		return projectName;
	}

	public static String getHostName() {
		InetAddress inetAddress = null;
		try {
			inetAddress = InetAddress.getLocalHost();
		} catch (final UnknownHostException e) {
			e.printStackTrace();
		}
		final String hostName = inetAddress.getHostName();
		return hostName;
	}

}
