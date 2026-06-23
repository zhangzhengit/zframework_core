package vo.zframework.event;

import java.util.EventObject;

/**
 *
 * 应用程序事件，所有需要被监听的事件类都要继承本类
 *
 * @author zhangzhen
 * @date 2023年11月14日
 *
 */
public abstract class ZApplicationEvent extends EventObject {

	private static final long serialVersionUID = -3307212834616401359L;

	/**
	 * 创建事件的时间戳
	 */
	private final long timestamp;

	public ZApplicationEvent(final Object source) {
		super(source);
		this.timestamp = System.currentTimeMillis();
	}

	public ZApplicationEvent(final Object source, final long timestamp) {
		super(source);
		this.timestamp = timestamp;
	}

	public long getTimestamp() {
		return timestamp;
	}

}
