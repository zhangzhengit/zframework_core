package com.vo.cache;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;

import com.vo.exception.CacheKeyDeclarationException;
import com.vo.scanner.ClassMap;

/**
 * cache包的校验器
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
public class ZCacheableValidator {

	public static void validated(final String... packageName) {
		final Set<Class<?>> csSet = ClassMap.scanPackage(packageName);

		for (final Class<?> cls : csSet) {
			final Method[] ms = cls.getDeclaredMethods();
			for (final Method m : ms) {
				int c = 0;
				final Annotation[] as = m.getAnnotations();

				for (final Annotation a : as) {
					if (a.annotationType().getCanonicalName().equals(ZCacheable.class.getCanonicalName())) {
						c++;
					}
					if (a.annotationType().getCanonicalName().equals(ZCachePut.class.getCanonicalName())) {
						c++;
					}
					if (a.annotationType().getCanonicalName().equals(ZCacheEvict.class.getCanonicalName())) {
						c++;
					}
				}
				if (c > 1) {
					throw new CacheKeyDeclarationException(

							"一个方法上 "
							+ "@" + ZCacheable.class.getSimpleName() + ","
							+ "@" + ZCachePut.class.getSimpleName() + ","
							+ "@" + ZCacheEvict.class.getSimpleName()
							+ " 只能存在一个,method = " + m.getName()

							);
				}

			}

		}
	}

}
