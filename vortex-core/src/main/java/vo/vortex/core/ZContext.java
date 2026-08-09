package vo.vortex.core;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import vo.vortex.exception.BeanAlreadyEexistsException;

/**
 * 存取 Bean。可使用addBean方法手动注入一个bean让容器管理，使用getBean方法获取一个由容器管理的bean
 *
 * @author zhangzhen
 * @date 2023年7月8日
 *
 */
public class ZContext {

	// FIXME 2026年6月13日 06:34:51 zhangzhen : 似乎没必要用con的，因为启动过程是单线程的，
	// 启动后就只有get操作了，所以整个生命周期中它都是安全的
	private static final ConcurrentMap<String, Object> BEAN_MAP = new ConcurrentHashMap<>(32, 1F);

	@SuppressWarnings("unchecked")
	public static <T> T getBean(final Class<T> beanClass) {
		return (T) getBean(gUK(beanClass));
	}

	public static <T> Object remove(final Class<T> beanClass) {
		return BEAN_MAP.remove(gUK(beanClass));
	}

	public static <T> String gUK(final Class<T> beanClass) {
		// return "ZContent_Bean-" + beanClass.getName();

		// c.get(beanClass)

		// FIXME 2024年12月23日 上午1:48:55 zhangzhen : 考虑好用什么比较好
		return beanClass.getName();
//		return beanClass.getName()+ "-bean";
		// return beanClass.getCanonicalName();
	}

	public static Object getBean(final String beanName) {
		return BEAN_MAP.get(beanName);
	}

	// FIXME 2026年7月28日 19:21:19 zhangzhen : addBeanAsync这个方法似乎不合理
	// 至少在启动过程中不该用，启动过程中可以并行，但是不应该异步，因为启动过程前后的对象
	// 有依赖关系，异步的话可能A没执行完，B依赖A但找不到，导致启动失败
	public static void addBeanAsync(final Class<?> beanClass, final Supplier<Object> supplier) {
//		Thread.ofVirtual().start(() -> addBean(gUK(beanClass), supplier.get()));
		new Thread(() -> addBean(gUK(beanClass), supplier.get())).start();
	}

	public static void addBean(final Class<?> beanClass, final Object bean) {
		addBean(gUK(beanClass), bean);
	}

	public static void addBean(final String beanName, final Object bean) {
		final Object v = BEAN_MAP.get(beanName);
		// 同样name已存在一个不同的
		if ((v != null) && (v != bean)) {
			throw new BeanAlreadyEexistsException(beanName);
		}
		BEAN_MAP.put(beanName, bean);
	}

	public static Map<String, Object> all() {
		return Collections.unmodifiableMap(BEAN_MAP);
	}

}
