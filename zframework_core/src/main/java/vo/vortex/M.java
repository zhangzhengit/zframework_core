package vo.vortex;

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

}
