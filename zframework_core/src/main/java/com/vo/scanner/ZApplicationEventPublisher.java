package com.vo.scanner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.HashBasedTable;
import com.vo.anno.ZAutowired;
import com.vo.anno.ZComponent;
import com.vo.core.ZContext;
import com.vo.exception.StartupException;
import com.votool.ze.ZE;

import cn.hutool.core.util.ArrayUtil;

/**
 * 事件发布者
 *
 * @author zhangzhen
 * @date 2023年11月14日
 *
 */
@ZComponent
public final class ZApplicationEventPublisher {

	private static final HashBasedTable<Class<? extends ZApplicationEvent>, Method, Class<?>> TABLE = HashBasedTable.create();

	private static final AtomicBoolean executed = new AtomicBoolean(false);

	@ZAutowired(name = "zeForApplicationEventPublisher")
	private ZE ze;

	/**
	 * 使用此方法来发布时一个事件，通知此事件的 @ZEventListener 来处理
	 *
	 * @param event
	 *
	 */
	public synchronized void publishEvent(final ZApplicationEvent event) {

		final Map<Method, Class<?>> row = TABLE.row(event.getClass());
		final Set<Entry<Method, Class<?>>> entrySet = row.entrySet();
		for (final Entry<Method, Class<?>> entry : entrySet) {
			final Object bean = ZContext.getBean(entry.getValue());
			if (bean == null) {
				continue;
			}

			this.invoke(entry.getKey(), bean, event);

		}

	}

	private void invoke(final Method method, final Object object, final ZApplicationEvent event) {

		this.ze.executeInQueue(() -> {
			try {
				method.invoke(object, event);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
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
		for (final Class<?> cls : csSet) {

			final Method[] ms = cls.getDeclaredMethods();
			for (final Method method : ms) {
				final ZEventListener eventListener = method.getAnnotation(ZEventListener.class);
				if (eventListener == null) {
					continue;
				}

				final Parameter[] ps = method.getParameters();
				if (ArrayUtil.isEmpty(ps) || ps.length != 1 || !ps[0].getType().equals(eventListener.value())) {
					throw new StartupException("@" + ZEventListener.class.getSimpleName() + "方法[" + cls.getSimpleName()
							+ "." + method.getName() + "]必须有且只有一个[" + eventListener.value().getSimpleName() + "]参数");
				}

				TABLE.put(eventListener.value(), method, cls);
			}
		}

		executed.set(true);
	}
}
