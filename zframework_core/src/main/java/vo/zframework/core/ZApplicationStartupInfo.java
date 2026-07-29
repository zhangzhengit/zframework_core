package vo.zframework.core;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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

	private final CompletableFuture<Set<Class<?>>> scanFuture;

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

	public Set<Class<?>> getPackageScanResult() {
		try {
			return this.scanFuture.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}

	public ZApplicationStartupInfo(final List<String> packageNameList, final boolean httpEnable, final String[] args,
			final CompletableFuture<Set<Class<?>>> scanFuture) {
		this.packageNameList = packageNameList;
		this.httpEnable = httpEnable;
		this.args = args;
		this.scanFuture = scanFuture;
	}

}
