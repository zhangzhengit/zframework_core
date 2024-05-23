package com.vo.scanner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vo.anno.ZOrder;
import com.vo.core.ZContext;
import com.vo.exception.StartupException;

import cn.hutool.core.collection.CollUtil;

/**
 * 扫描 ZHandlerInterceptor 的实现类
 *
 * @author zhangzhen
 * @date 2023年11月15日
 *
 */
public class ZHandlerInterceptorScanner {

	private static final List<ZHandlerInterceptor> BEAN_LIST = Lists.newArrayList();

	/**
	 * 扫描 ZHandlerInterceptor 的实现类
	 *
	 */
	public static void scan() {

		final ImmutableCollection<Object> values = ZContext.all().values();
		final HashSet<Integer> ovSet = Sets.newHashSet();
		for (final Object bean : values) {

			final Optional<Class<?>> findAny = Arrays.stream(bean.getClass().getInterfaces())
					.filter(i -> i.equals(ZHandlerInterceptor.class)).findAny();
			if (findAny.isPresent()) {
				BEAN_LIST.add((ZHandlerInterceptor) bean);

				final ZHandlerInterceptor zhi = (ZHandlerInterceptor) bean;

				final boolean annotationPresent = bean.getClass().isAnnotationPresent(ZOrder.class);
				if (!annotationPresent) {
					throw new StartupException("拦截器 " + bean.getClass().getSimpleName() + " 必须使用 @"
							+ ZOrder.class.getSimpleName() + " 注解");
				}

				final int value = bean.getClass().getAnnotation(ZOrder.class).value();
				final boolean add = ovSet.add(value);
				if (!add) {
					throw new StartupException("拦截器 " + bean.getClass().getSimpleName() + " @"
							+ ZOrder.class.getSimpleName() + ".value [" + value + "] 值重复" );
				}
			}

		}

	}

	/**
	 * 获取 ZHandlerInterceptor 的实现类
	 *
	 * @return
	 *
	 */
	public static List<ZHandlerInterceptor> get() {
		return BEAN_LIST;
	}

	/**
	 * 返回匹配请求路径的拦截器，已经按从前到后的执行顺序排序了
	 *
	 * @param requestURI 请求路径,如：/index、/user/1
	 * @return
	 *
	 */
	public static List<ZHandlerInterceptor> match(final String requestURI) {

		synchronized (requestURI.intern()) {
			final List<ZHandlerInterceptor> v = map.get(requestURI);
			if (v != null) {
				return v;
			}

			final List<ZHandlerInterceptor> v2 = match0(requestURI);
			map.put(requestURI, v2);
			return v2;
		}

	}

	static Map<String, List<ZHandlerInterceptor>> map = new WeakHashMap<>(4,1F);

	private static List<ZHandlerInterceptor> match0(final String requestURI) {
		final List<ZHandlerInterceptor> zhiRList = Lists.newArrayList();

		final List<ZHandlerInterceptor> list = get();
		if (CollUtil.isNotEmpty(list)) {
			for (final ZHandlerInterceptor zhi : list) {
				final String[] interceptionPath = zhi.interceptionPathRegex();
				for (final String reg : interceptionPath) {
					final Pattern pattern = Pattern.compile(reg);
					final Matcher matcher = pattern.matcher(requestURI);
					if (matcher.matches()) {
						zhiRList.add(zhi);
					}
				}
			}
		}

		zhiRList.sort((o1, o2) -> {
			final int value1 = o1.getClass().getAnnotation(ZOrder.class).value();
			final int value2 = o2.getClass().getAnnotation(ZOrder.class).value();
			return Integer.compare(value1, value2);
		});

		return zhiRList;
	}
}
