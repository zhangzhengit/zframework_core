package vo.zframework.http;

import java.util.concurrent.atomic.AtomicLong;

import vo.zframework.cache.ZRC;
import vo.zframework.enums.QPSHandlingEnum;

/**
 * 问deepseek要的代码：
 *
 * 秒级限流器（支持平滑/突发两种模式）
 *
 * @author zhangzhen
 * @date 2026年7月14日
 */
public class QC {

	private static final ZRC STATE_MAP = new ZRC(10000 * 10, 2);

	/**
	 * 限流入口
	 *
	 * @param keyPrefix    限流 key（例如接口路径）
	 * @param qps          每秒允许的请求数
	 * @param handlingEnum SMOOTH（平滑） 或 UNEVEN（突发）
	 * @return true 允许通过，false 拒绝
	 */
	public static boolean allow(final String keyPrefix, final long qps, final QPSHandlingEnum handlingEnum) {
		if (qps <= 0) {
			return false;
		}

		final LimiterState state = STATE_MAP.computeIfAbsent(keyPrefix, () -> {
			if (handlingEnum == QPSHandlingEnum.SMOOTH) {
				return new SmoothLimiterState(qps);
			}
			return new UnevenLimiterState(qps);
		});

		// 如果限流模式与已有状态不一致，需要重建（但这里简化处理，首次创建后固定）
		return state.allow();
	}

	// ================== 内部状态接口 ==================
	private interface LimiterState {
		boolean allow();
	}

	// ================== 突发模式（固定窗口） ==================
	private static class UnevenLimiterState implements LimiterState {
		private final AtomicLong count = new AtomicLong(0);
		private final AtomicLong window = new AtomicLong(0);
		private final long qps;

		UnevenLimiterState(final long qps) {
			this.qps = qps;
		}

		@Override
		public boolean allow() {
			final long now = System.currentTimeMillis() / 1000;
			final long currentWindow = this.window.get();

			// 窗口切换时重置
			if (currentWindow != now) {
				if (!this.window.compareAndSet(currentWindow, now)) {
					// 其他线程已重置，重试
					return this.allow();
				}
				this.count.set(0);
			}

			// 自旋递增计数
			while (true) {
				final long c = this.count.get();
				if (c >= this.qps) {
					return false;
				}
				if (this.count.compareAndSet(c, c + 1)) {
					return true;
				}
			}
		}
	}

	// ================== 平滑模式（令牌桶） ==================
	private static class SmoothLimiterState implements LimiterState {
		private final AtomicLong tokens = new AtomicLong(0);
		private final AtomicLong lastRefillTime = new AtomicLong(System.currentTimeMillis());
		private final long qps;

		SmoothLimiterState(final long qps) {
			this.qps = qps;
		}

		@Override
		public boolean allow() {
			final long now = System.currentTimeMillis();
			final long last = this.lastRefillTime.get();

			// 计算本秒内应生成的令牌数
			final long elapsed = now - last;
			final long newTokens = (elapsed * this.qps) / 1000; // 每毫秒生成 qps/1000 个令牌
			if (newTokens > 0) {
				// 尝试更新最后填充时间（只有成功更新才实际增加令牌）
				if (!this.lastRefillTime.compareAndSet(last, now)) {
					// 其他线程已经更新，重试
					return this.allow();
				}
				final long current = this.tokens.get();
				final long refilled = Math.min(current + newTokens, this.qps); // 不超过容量
				this.tokens.compareAndSet(current, refilled);
			}

			// 尝试消耗令牌
			while (true) {
				final long currentTokens = this.tokens.get();
				if (currentTokens <= 0) {
					return false;
				}
				if (this.tokens.compareAndSet(currentTokens, currentTokens - 1)) {
					return true;
				}
			}
		}
	}
}