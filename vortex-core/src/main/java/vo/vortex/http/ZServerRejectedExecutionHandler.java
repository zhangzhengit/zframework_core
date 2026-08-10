package vo.vortex.http;

import java.time.LocalDateTime;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 平台线程池拒绝策略
 *
 * @author zhangzhen
 * @date 2026年8月10日 07:53:32
 */
public class ZServerRejectedExecutionHandler implements RejectedExecutionHandler{

	@Override
	public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
		System.out.println(LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
				+ "ZServerRejectedExecutionHandler.rejectedExecution()");
	}

}
