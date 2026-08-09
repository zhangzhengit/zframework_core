package vo.vortex.http;

import java.util.concurrent.atomic.AtomicLong;

import vo.vortex.enums.QCTimeEnum;

/**
 *
 * 限流器
 *
 * @author zhangzhen
 * @date 2026年7月14日 14:40:11
 */
public class QC2 {
	private final AtomicLong count = new AtomicLong(0L);
	private final AtomicLong time = new AtomicLong(0L);
	private final long qps;
	private final QCTimeEnum qcTimeEnum;

	public QC2(final String keyword, final long qps, final QCTimeEnum qcTimeEnum) {
		if (qps <= 0) {
			throw new IllegalArgumentException("qps 必须>0");
		}
		this.qps = qps;
		this.qcTimeEnum = qcTimeEnum;
	}

	public boolean allow() {
		final long time = this.qcTimeEnum.convert(System.currentTimeMillis());
		final long currentTime = this.time.get();

		if (currentTime != time) {
			if (this.time.compareAndSet(currentTime, time)) {
				this.count.set(0L);
			} else {
				if (this.time.get() != time) {
					return this.allow();
				}
			}
		}

		while (true) {
			final long currentCount = this.count.get();
			if (currentCount >= this.qps) {
				return false;
			}

			if (this.count.compareAndSet(currentCount, currentCount + 1)) {
				return true;
			}
		}
	}
}
