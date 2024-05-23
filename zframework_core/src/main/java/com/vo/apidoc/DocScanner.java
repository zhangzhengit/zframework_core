package com.vo.apidoc;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

import com.vo.anno.ZController;
import com.vo.core.ZLog2;
import com.vo.enums.MethodEnum;
import com.vo.http.ZRequestMapping;
import com.vo.scanner.ZConfigurationPropertiesScanner;

import cn.hutool.core.collection.CollUtil;

/**
 * 扫描 @ZController 类里的 API方法，生成接口文档
 *
 * @author zhangzhen
 * @data 2024年5月23日 下午8:50:16
 *
 */
public class DocScanner {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static void scan(final String... packageName)  {

		final Set<Class<?>> zcSet = ZConfigurationPropertiesScanner.scanPackage(packageName).stream()
				.filter(cls -> cls.isAnnotationPresent(ZController.class))
				.collect(Collectors.toSet());

		if (CollUtil.isEmpty(zcSet)) {
			return;
		}

		for (final Class<?> zc : zcSet) {
			final Method[] ms = zc.getDeclaredMethods();
			for (final Method m : ms) {
				final ZRequestMapping zrm = m.getAnnotation(ZRequestMapping.class);
				if(zrm==null) {
					continue;
				}

				final String description = zrm.description();
				final MethodEnum method = zrm.method();

				// FIXME 2024年5月23日 下午8:54:12 zhangzhen: doc 写这里，在启动时调用，放到一个地方，再写一个API展示出来
			}

		}
	}

}
