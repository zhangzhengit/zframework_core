package com.vo.zframework.scanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import com.vo.zframework.anno.ZAsync;
import com.vo.zframework.aop.ZAsyncRV;
import com.vo.zframework.exception.StartupException;

import vo.log.core.ZLog2;

/**
 * @ZAsync 启动流程
 *
 * @author zhangzhen
 * @date 2026年5月4日 08:05:19
 */
public class ZAsyncScanner {

	private static final ZLog2 LOG = ZLog2.getInstance();

	private static final Class<ZAsync> TYPE = ZAsync.class;

	/**
	 * @param annoClass
	 * @param packageName
	 * @return
	 */
	public static Set<Class<?>> scan(final Class<? extends Annotation> annoClass,
			final String... packageName) {

//		LOG.info("开始扫描带有[{}]注解的类", annoClass.getCanonicalName());
		final Set<Class<?>> zcSet = ClassMap.scanPackageByAnnotation(annoClass,
				packageName);

		for (final Class<?> cls : zcSet) {
			final Method[] ms = cls.getDeclaredMethods();
			for (final Method method : ms) {
				final ZAsync async = method.getAnnotation(TYPE);
				if (async == null) {
					continue;
				}

				vIsPublic(cls, method);

				final Class<?> mRT = method.getReturnType();
				if (mRT == Void.TYPE) {
					continue;
				}

				vMRTIsZARV(cls, method, mRT);

			}
		}

		return zcSet;
	}

	private static void vMRTIsZARV(final Class<?> cls, final Method method, final Class<?> mRT) {
		if (mRT != ZAsyncRV.class) {
			final String me =
					"\r\n\t"
					+ "@" + TYPE.getSimpleName()
					+ "方法声明错误："
					+ "\r\n\t"
					+ cls.getName() + "." + method.getName()
					+ "\r\n\t"
					+ "返回类型:[" + mRT.getCanonicalName() + "]"
					+ "\r\n\t"
					+ "如果方法有返回值,必须返回[" + ZAsyncRV.class.getCanonicalName() + "]类型"
					+ "\r\n\t"
					+ "请修改代码："
					+ "\r\n\t"
					+ "1、删除方法上的@"
					+ TYPE.getSimpleName()
					+ "\r\n\t"
					+ "2、修改返回类型为[" + ZAsyncRV.class.getSimpleName() + "<"+mRT.getSimpleName()+">]的形式,"
					+ "并且使用"+ZAsyncRV.class.getSimpleName()+".ok(final T v)方法来构造返回值"
					+ "\r\n\t"
					;
			throw new StartupException(me);
		}
	}

	private static void vIsPublic(final Class<?> cls, final Method method) {
		if (!Modifier.isPublic(method.getModifiers())) {
			final String me =
					"\r\n\t"
					+ "@" + TYPE.getSimpleName()
					+ "方法声明错误："
					+ "\r\n\t"
					+ cls.getName() + "." + method.getName()
					+ "\r\n\t"
					+ "带有@" + TYPE.getSimpleName() + "的方法必须声明为public"
					+ "\r\n\t"
					+ "请修改代码："
					+ "\r\n\t"
					+ "1、去除@" + TYPE.getSimpleName()
					+ "\r\n\t"
					+ "2、修改方法为public"
					+ "\r\n\t"
					;
			throw new StartupException(me);
		}
	}

}
