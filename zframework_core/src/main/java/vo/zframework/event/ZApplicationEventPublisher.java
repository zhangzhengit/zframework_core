package vo.zframework.event;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import vo.zframework.anno.ZComponent;
import vo.zframework.common.AU;
import vo.zframework.common.ZHashBasedTable;
import vo.zframework.core.ZContext;
import vo.zframework.exception.StartupException;
import vo.zframework.scanner.ClassMap;
import vo.zframework.zclass.ZClass;
import vo.zframework.zclass.ZMethod;
import vo.zframework.zclass.ZMethodArg;
import vo.zframework.zclass.ZPackage;

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

	// FIXME 2026年7月16日 09:25:34 zhangzhen : 改为静态调用后，这个TABLE应该可以去掉，待会再看
	private static final ZHashBasedTable<Class<? extends ZApplicationEvent>, Method, Class<?>> TABLE = new ZHashBasedTable<>();

	private static final AtomicBoolean executed = new AtomicBoolean(false);

	/**
	 * 使用此方法来发布时一个事件，通知此事件的 @ZEventListener 来处理
	 *
	 * @param event
	 *
	 */
	public void publishEvent(final ZApplicationEvent event) {
		// 2
		ves.execute(() -> {
			Thread.currentThread().setName(TRREAD_NAME + VT_N.incrementAndGet());
			final IRoute route = ZContext.getBean(IRoute.class);
			route.route(event);
		});

		// 1
//		final Map<Method, Class<?>> row = TABLE.row(event.getClass());
//		final Set<Entry<Method, Class<?>>> entrySet = row.entrySet();
//		for (final Entry<Method, Class<?>> entry : entrySet) {
//			final Object bean = ZContext.getBean(entry.getValue());
//			if (bean != null) {
//				ZApplicationEventPublisher.invoke(entry.getKey(), bean, event);
//			}
//		}

	}

//	private static void invoke(final Method method, final Object object, final ZApplicationEvent event) {
//
//		ves.execute(() ->{
//
//			Thread.currentThread().setName(TRREAD_NAME + VT_N.incrementAndGet());
//
//			try {
//				method.invoke(object, event);
//			} catch (IllegalAccessException | InvocationTargetException e) {
//				e.printStackTrace();
//			}
//		});
//
//	}

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

		final ZClass proxyZClass = new ZClass();
		proxyZClass.setPackage1(new ZPackage("vo.zframework.generated"));
		proxyZClass.setName("ZApplicationEventRoute");

		proxyZClass.setImplementsSet(Set.of(IRoute.class.getCanonicalName()));

		final ZMethod routeMethod = new ZMethod();
		routeMethod.setName("route");
		routeMethod.setMethodArgList(List.of(new ZMethodArg(ZApplicationEvent.class, "event")));

		proxyZClass.setMethodSet(Set.of(routeMethod));

		final StringBuilder routeBody = new StringBuilder(
				 "String canonicalName = event.getClass().getCanonicalName();"
					+ "switch (canonicalName) {");

		final Set<Class<? extends ZApplicationEvent>> rowKeySet = TABLE.rowKeySet();

		int pI = 0;
		for (final Class<? extends ZApplicationEvent> class1 : rowKeySet) {
			final Map<Method, Class<?>> row = TABLE.row(class1);
			final Set<Entry<Method, Class<?>>> es = row.entrySet();


			final Collection<Class<?>> values = row.values();
			final Set<Class<?>> set = new HashSet<>(values);
			for (final Class<?> cls : set) {
				pI++;

				final List<Entry<Method, Class<?>>> ml = es.stream().filter(e -> e.getValue().equals(cls)).collect(Collectors.toList());

				final String eName = class1.getCanonicalName();

				routeBody.append("case \"").append(eName).append("\"").append(':');

				final String clscanonicalName = cls.getCanonicalName();
				routeBody.append(clscanonicalName)

				.append(" p").append(pI).append(" = (").append(clscanonicalName)
				.append(")")
				.append(ZContext.class.getCanonicalName())
				.append(".getBean(\"")
				.append(cls.getCanonicalName()).append("\");");

				final String name = class1.getName();

				for (final Entry<Method, Class<?>> e : ml) {
					final Method method = e.getKey();
					routeBody
					.append("p").append(pI).append('.').append(method.getName())
					.append("(")
					.append("(").append(name).append(")")
					.append("event")
					.append(");");
				}
			}

			routeBody.append("break;");
		}

		routeBody.append("default:\r\n"
							+ "	break;\r\n"
							+ "}	");

		routeMethod.setBody(routeBody.toString());

		ZContext.addBean(IRoute.class, proxyZClass.newInstance());

		executed.set(true);
	}
}
