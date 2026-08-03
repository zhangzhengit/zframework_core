package vo.vortex.event;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import vo.vortex.anno.ZComponent;
import vo.vortex.anno.ZEventListener;
import vo.vortex.common.AU;
import vo.vortex.common.ZHashBasedTable;
import vo.vortex.core.ZApplicationStartupInfo;
import vo.vortex.core.ZContext;
import vo.vortex.exception.StartupException;
import vo.vortex.g.APT;
import vo.vortex.g.G;
import vo.vortex.route.IEventRoute;
import vo.vortex.zclass.ZClass;
import vo.vortex.zclass.ZMethod;
import vo.vortex.zclass.ZMethodArg;
import vo.vortex.zclass.ZPackage;

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

	private static final String CLASS_NAME = "vo.vortex.generated.maven.ZApplicationEventRoute";

	private static final ExecutorService ves = Executors.newVirtualThreadPerTaskExecutor();

	// FIXME 2026年7月17日 06:21:59 zhangzhen : 排除了这两个包名前缀，为了加快启动速度，因为当前的实现这两个包下无监听器
	// 但是应该提示用户不可以把事件监听器放在这两个包下,不然就扫描不到了
	private static final String VO_VORTEX = "vo.vortex";
	private static final String VO_LOG = "vo.log";

	private static final AtomicLong VT_N = new AtomicLong(0L);

	private static boolean executed = false;

	/**
	 * 使用此方法来发布一个事件，通知此事件的 @ZEventListener 来处理
	 *
	 * @param event
	 *
	 */
	public void publishEvent(final ZApplicationEvent event) {
		ves.execute(() -> {
			Thread.currentThread().setName(TRREAD_NAME + VT_N.incrementAndGet());
			final IEventRoute route = ZContext.getBean(IEventRoute.class);
			route.route(event);
		});
	}

	/**
	 * 使用此方法来发布多个事件，通知此事件的 @ZEventListener 来处理
	 *
	 * @param events
	 */
	public void publishEvent(final ZApplicationEvent... events) {
		if (AU.isEmpty(events)) {
			return;
		}

		for (final ZApplicationEvent e : events) {
			if (e == null) {
				continue;
			}
			this.publishEvent(e);
		}
	}

	public synchronized static void start(final ZApplicationStartupInfo startupInfo, final Set<Class<?>> allClass) {

		if (executed) {
			return;
		}

		// 2
//		final Set<Class<?>> clsSet = allClass;

		// 2
		final Set<Class<?>> clsSet = APT.getAllClass();
		final Set<Class<?>> x = G.getAllClass();
		clsSet.addAll(x);

		// 1
//		final Set<Class<?>> clsSet = ClassMap.scanPackage(startupInfo.getPackageNameArray());

		final Set<Class<?>> noVOZFClsSet = clsSet
			.parallelStream()
			// FIXME 2026年8月2日 09:37:11 zhangzhen : 先cs -> cs != null这样，记得改
			.filter(cs -> cs != null)
			// FIXME 2026年6月23日 15:36:40 zhangzhen : 因为当前没有内置的 @ZEventListener，所以把
			// vo.zframework.XX和vo.log.XX包全排除，当然最好是精准匹配每个包名，暂时先这样
			.filter(cs -> !cs.getPackageName().startsWith(VO_VORTEX))
			.filter(cs -> !cs.getPackageName().startsWith(VO_LOG))
			.collect(Collectors.toSet());

		// FIXME 2026年7月17日 06:20:52 zhangzhen : 从常量改为局部的了，应该可以继续改，先暂时这样吧
		final ZHashBasedTable<Class<? extends ZApplicationEvent>, Method, Class<?>> table = new ZHashBasedTable<>();

		noVOZFClsSet
		.parallelStream()
		.forEach(cls ->{

			final Method[] ms = cls.getDeclaredMethods();

			for (final Method method : ms) {
				final ZEventListener eventListener = method.getAnnotation(ZEventListener.class);
				if (eventListener == null) {
					continue;
				}

				if ((method.getParameterCount() != 1)
				|| !method.getParameterTypes()[0].equals(eventListener.value())) {
					throw new StartupException("@" + ZEventListener.class.getSimpleName() + "方法[" + cls.getSimpleName()
							+ "." + method.getName() + "]必须有且只有一个[" + eventListener.value().getSimpleName() + "]参数");
				}

				synchronized (table) {
					table.put(eventListener.value(), method, cls);
				}
			}

		});

		final Object proxyZClassInstance = G.newInstance(G.load(CLASS_NAME));
		ZContext.addBean(IEventRoute.class, proxyZClassInstance);

		executed = true;
	}
}
