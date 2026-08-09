package vo.vortex.core;

/**
 * 时间单位，给限流器用
 *
 * @author zhangzhen
 * @date 2025年1月21日 下午9:32:58
 *
 */
public enum QCTimeEnum {

	/**
	 * 秒
	 */
	SECOND {
		@Override
		public long convert(final long currentTimeMillis) {
			return currentTimeMillis / 1000;
		}
	},

	/**
	 * 分
	 */
	MINUTE {
		@Override
		public long convert(final long currentTimeMillis) {
			return SECOND.convert(currentTimeMillis) / 60;
		}
	},

	/**
	 * 一刻钟
	 */
	QUARTER {
		@Override
		public long convert(final long currentTimeMillis) {
			return MINUTE.convert(currentTimeMillis) / 15;
		}
	},

	/**
	 * 小时
	 */
	HOUR {
		@Override
		public long convert(final long currentTimeMillis) {
			return MINUTE.convert(currentTimeMillis) / 60;
		}
	},;

	public abstract long convert(long currentTimeMillis);

}
