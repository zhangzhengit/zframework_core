package vo.zframework.cache;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.List;

import vo.zframework.anno.ZAutowired;
import vo.zframework.aop.AOPParameter;
import vo.zframework.aop.ZAOP;
import vo.zframework.aop.ZIAOP;
import vo.zframework.core.RU;
import vo.zframework.exception.CacheKeyDeclarationException;

/**
 * @ZCacheable 的AOP实现类
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@ZAOP(interceptType = ZCacheable.class)
public class ZCacheableAOP implements ZIAOP {

	private static final String separatorChar = ".";

	public static final String PREFIX = "ZCacheable";

	@ZAutowired(name = ZCache.CACHE_BBUILTIN_FOR_PACKAGE_CACHE)
	private ZCache<ZCacheR> cache;

	@ZAutowired
	private ZCacheConfigurationProperties cacheConfigurationProperties;

	@Override
	public Object before(final AOPParameter aopParameter) {
		return null;
	}

	@Override
	public Object around(final AOPParameter aopParameter) {
		// FIXME 2025年1月17日 下午11:48:24 zhangzhen : 这个偶尔NPE，查找原因
		if (!this.cacheConfigurationProperties.getEnable()) {
			return aopParameter.invoke();
		}

		final ZCacheable annotation = aopParameter.getMethod().getAnnotation(ZCacheable.class);

		final String key = annotation.key();

		final String cacheKey = ZCacheableAOP.gKey(aopParameter, key, annotation.group());
		final ZCacheR vC = this.cache.get(cacheKey);
		if (vC != null) {
			return vC.getValue();
		}

		synchronized (cacheKey.intern()) {
			// 后面排队的线程开始执行后，先判断下缓存内是否已经有结果了（是否前面的线程已经把结果放入了）。
			final ZCacheR vC2 = this.cache.get(cacheKey);
			if (vC2 != null) {
				return vC2.getValue();
			}

			final Object v = aopParameter.invoke();
			final ZCacheR r = new ZCacheR(cacheKey, v, annotation.expire(),
					System.currentTimeMillis());

			this.cache.add(cacheKey, r, annotation.expire());

			return v;
		}
	}

	@Override
	public Object after(final AOPParameter aopParameter) {
		return null;
	}

	// FIXME 2025年1月22日 下午4:16:17 zhangzhen :
	// 这个方法也比较耗时，尤其hash方法特别耗时并且导致key不可读，并且key已经够长了可以自描述了。记得改短并且可读，三个注解都改
	static String gKey(final AOPParameter aopParameter, final String key, final String group) {
		if (STU.isNullOrEmptyOrBlank(key)) {
			return ZCacheableAOP.PREFIX + "@" + aopParameter.getTarget().getClass().getCanonicalName() + "@" + group;
		}

		final Parameter[] ps = RU.getParameters(aopParameter.getMethod());
		if (AU.isEmpty(ps)) {
			return PREFIX + "@" + group + "@" + key;
		}

		final int ix = key.indexOf(separatorChar);
		if (ix < 0) {
			for (int i = 0; i < ps.length; i++) {

				final Parameter parameter = ps[i];
				if (parameter.getName().equals(key)) {

					final String canonicalName = aopParameter.getTarget().getClass().getName();
					final List<Object> pl = aopParameter.getParameterList();
					return PREFIX + "@" + canonicalName + "@" + group + "@" + parameter.getName() + STU.EQUALS
							+ gKey(pl.get(i));
				}
			}

			throw new CacheKeyDeclarationException(
					"key不存在,key = " + key + ",方法名称=" + aopParameter.getMethod().getName());
		}

		if ((ix == 0) || (ix == (key.length() - 1))) {
			throw new CacheKeyDeclarationException("key声明异常,key = " + key + ",方法名称=" + aopParameter.getMethod().getName()
					+ ",请声明为[方法参数名.字段名]的形式,如：user.id"
					);
		}

		final String pname = key.substring(0, ix);
		final String fname = key.substring(ix + separatorChar.length());

		for (int i = 0; i < ps.length; i++) {

			final Parameter parameter = ps[i];
			if (parameter.getName().equals(pname)) {

				final String canonicalName = aopParameter.getTarget().getClass().getName();
				final List<Object> pl = aopParameter.getParameterList();
				final Object pO = pl.get(i);

				final Field f = RU.getDeclaredField(pO, fname);
				final Object fV = RU.getFiledValue(pO, f);

				return PREFIX + "@" + canonicalName + "@" + group + "@" + parameter.getName() + STU.EQUALS
						+ fV;
			}
		}

		throw new CacheKeyDeclarationException("key不存在,key = " + key + ",方法名称=" + aopParameter.getMethod().getName());
	}

	public static String gKey(final Object object) {
		return String.valueOf(object);
	}
}
