package vo.vortex.apidoc;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import vo.log.core.ZLog2;
import vo.vortex.anno.ZRestController;
import vo.vortex.cache.CU;
import vo.vortex.cache.STU;
import vo.vortex.core.ZRequest;
import vo.vortex.core.ZResponse;
import vo.vortex.enums.MethodEnum;
import vo.vortex.http.ZRequestMapping;
import vo.vortex.scanner.ZConfigurationPropertiesScanner;
import vo.vortex.template.ZModel;

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
				.filter(cls -> cls.isAnnotationPresent(ZRestController.class))
				.collect(Collectors.toSet());

		if (CU.isEmpty(zcSet)) {
			return;
		}

		for (final Class<?> zc : zcSet) {

			final Method[] ms = zc.getDeclaredMethods();
			for (final Method m : ms) {
				final ZRequestMapping zrm = m.getAnnotation(ZRequestMapping.class);
				if (zrm == null) {
					continue;
				}

				final String canonicalName = m.getReturnType().getCanonicalName();
				final String description = zrm.description();
				final MethodEnum method = zrm.method();
				final String[] mapping = zrm.mapping();

				final String mS = Arrays.toString(mapping);



				final Parameter[] parameters = m.getParameters();
				final StringJoiner joiner = new StringJoiner(",");
				for (final Parameter p : parameters) {
					if(p.getType().equals(ZModel.class)
							|| p.getType().equals(ZRequest.class)
							|| p.getType().equals(ZResponse.class)
							) {
						continue;
					}
					final String name = p.getName();
					final String type = p.getType().getSimpleName();
					joiner.add(type + STU.SAPCE +name);
				}
				//				System.out.println(description);
				//				System.out.println(method + " " + mS);
				//				System.out.println(joiner);
				//				System.out.println();

				// FIXME 2024年12月17日 下午6:33:56 zhangzhen : 写这里 APIInfo 放进去，再写一个接口，返回html把这些api展示出来

				// FIXME 2024年5月23日 下午8:54:12 zhangzhen: doc 写这里，在启动时调用，放到一个地方，再写一个API展示出来
			}

		}
	}

}
