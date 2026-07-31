package vo.zframework;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import vo.zframework.enums.OSEnum;

/**
 *	本类要放在顶级包名下
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
		try {
			final InetAddress inetAddress = InetAddress.getLocalHost();
			final String hostName = inetAddress.getHostName();
			return hostName;
		} catch (final UnknownHostException e) {
			e.printStackTrace();
		}

		return null;
	}

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
