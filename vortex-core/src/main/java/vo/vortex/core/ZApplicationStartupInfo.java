package vo.vortex.core;

import java.util.List;

/**
 * 程序启动信息
 *
 * @author zhangzhen
 * @date 2023年12月4日
 *
 */
public class ZApplicationStartupInfo {

	private final List<String> packageNameList;
	private final boolean httpEnable;

	private final String[] args;

	public List<String> getPackageNameList() {
		return this.packageNameList;
	}

	public String[] getPackageNameArray() {
		return this.getPackageNameList().toArray(new String[0]);
	}

	public boolean isHttpEnable() {
		return this.httpEnable;
	}

	public String[] getArgs() {
		return this.args;
	}

	public ZApplicationStartupInfo(final List<String> packageNameList, final boolean httpEnable, final String[] args) {
		this.packageNameList = packageNameList;
		this.httpEnable = httpEnable;
		this.args = args;
	}

}
