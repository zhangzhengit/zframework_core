package vo.zframework.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.google.common.collect.HashBasedTable;

import vo.zframework.anno.ZComponent;
import vo.zframework.common.AU;
import vo.zframework.core.ZContext;
import vo.zframework.exception.StartupException;
import vo.zframework.scanner.ClassMap;

/**
 * 事件发布者
 *
 * @author zhangzhen
 * @date 2023年11月14日
 *
 */
@ZComponent
public final class ZApplicationEventPublisher {

	private static final String TRREAD_NAME = "aeT-";

	private static final ExecutorService ves = Executors.newVirtualThreadPerTaskExecutor();

	private static final String VO_ZFRAMEWORK = "vo.zframework";
	private static final String VO_LOG = "vo.log";

	private static final AtomicLong VT_N = new AtomicLong(0L);

	private static final HashBasedTable<Class<? extends ZApplicationEvent>, Method, Class<?>> TABLE = HashBasedTable.create();

	private static final AtomicBoolean executed = new AtomicBoolean(false);

	/**
	 * 使用此方法来发布时一个事件，通知此事件的 @ZEventListener 来处理
	 *
	 * @param event
	 *
	 */
	public void publishEvent(final ZApplicationEvent event) {

		final Map<Method, Class<?>> row = TABLE.row(event.getClass());
		final Set<Entry<Method, Class<?>>> entrySet = row.entrySet();
		for (final Entry<Method, Class<?>> entry : entrySet) {
			final Object bean = ZContext.getBean(entry.getValue());
			if (bean != null) {
				ZApplicationEventPublisher.invoke(entry.getKey(), bean, event);
			}
		}

	}

	private static void invoke(final Method method, final Object object, final ZApplicationEvent event) {

		ves.execute(() ->{

			Thread.currentThread().setName(TRREAD_NAME + VT_N.incrementAndGet());

			try {
				method.invoke(object, event);
			} catch (IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
			}
		});

	}

	public void publishEvent(final ZApplicationEvent... events) {
		for (final ZApplicationEvent e : events) {
			this.publishEvent(e);
		}
	}

	public synchronized static void start(final String... packageName) {

		if (executed.get()) {
			return;
		}

		final Set<Class<?>> csSet = ClassMap.scanPackage(packageName);

		final Set<Class<?>> noVOZFClsSet = csSet
			.parallelStream()
			// FIXME 2026年6月23日 15:36:40 zhangzhen : 因为当前没有内置的 @ZEventListener，所以把
			// vo.zframework.XX和vo.log.XX包全排除，当然最好是精准匹配每个包名，暂时先这样
			.filter(cs -> !cs.getPackageName().startsWith(VO_ZFRAMEWORK))
			.filter(cs -> !cs.getPackageName().startsWith(VO_LOG))
			.collect(Collectors.toSet());

		for (final Class<?> cls : noVOZFClsSet) {

			final Method[] ms = cls.getDeclaredMethods();

			for (final Method method : ms) {
				final ZEventListener eventListener = method.getAnnotation(ZEventListener.class);
				if (eventListener == null) {
					continue;
				}

				final Parameter[] ps = method.getParameters();
				if (AU.isEmpty(ps) || (ps.length != 1) || !ps[0].getType().equals(eventListener.value())) {
					throw new StartupException("@" + ZEventListener.class.getSimpleName() + "方法[" + cls.getSimpleName()
							+ "." + method.getName() + "]必须有且只有一个[" + eventListener.value().getSimpleName() + "]参数");
				}

				TABLE.put(eventListener.value(), method, cls);
			}
		}

		executed.set(true);
	}
}
