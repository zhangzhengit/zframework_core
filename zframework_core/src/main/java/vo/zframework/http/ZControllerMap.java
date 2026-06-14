package vo.zframework.http;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashBasedTable;

import vo.zframework.cache.STU;
import vo.zframework.core.QPSEnum;
import vo.zframework.core.ZMultipartFile;
import vo.zframework.enums.MethodEnum;
import vo.zframework.exception.StartupException;

/**
 * 存取接口方法
 *
 * @author zhangzhen
 * @date 2023年6月28日
 *
 */
public class ZControllerMap {
	static final HashBasedTable<String, String, ZRMethod> methodPathTable = HashBasedTable.create();
	static final HashBasedTable<Method, String, Boolean> methodIsregexTable = HashBasedTable.create();
	static final HashMap<Method, Object> objectMap = new HashMap<>(16, 1F);
	static final HashSet<String> mappingSet = new HashSet<>();

	/**
	 * 注册一个接口
	 *
	 * @param methodEnum 接口请求方法，如： MethodEnum.POST
	 * @param mapping    匹配路径，如：/index
	 * @param method     具体的接口方法
	 * @param cte
	 * @param zcObject     接口方法所在的对象
	 * @param isRegex    mapping 是否正则表达式
	 */
	public synchronized static void put(final MethodEnum methodEnum, final String mapping, final Method method,
			final CTEnum cte, final Object zcObject, final boolean isRegex) {

		final ZRequestMapping requestMapping = method.getAnnotation(ZRequestMapping.class);

		checkAPI(methodEnum, mapping, method, zcObject, requestMapping);

		final Parameter[] ps = method.getParameters();
		for (final Parameter pp : ps) {
			if(pp.getType().equals(ZMultipartFile.class) && ((methodEnum != MethodEnum.POST)
					&& (methodEnum != MethodEnum.PUT)
					&& (methodEnum != MethodEnum.PATCH))
					) {


				throw new StartupException(
						"接口 " + method.getName() + " 带有 " + ZMultipartFile.class.getSimpleName() + " 参数，请改为 "

							+ MethodEnum.POST.name() + "/" + MethodEnum.PUT.name() + "/" + MethodEnum.PATCH.name()

						);

			}

		}

		methodPathTable.put(methodEnum.name(), mapping, new ZRMethod(method, cte, zcObject));

		methodIsregexTable.put(method, mapping, isRegex);

		objectMap.put(method, zcObject);

		//		final ZRequestMappingConfigurationProperties zrmConf = ZContext.getBean(ZRequestMappingConfigurationProperties.class);
		final int count = requestMapping.count() == ZRequestMapping.DEFAULT_COUNT ? ZRequestMapping.DEFAULT_COUNT : requestMapping.count();
		if (count <= 0) {
			throw new StartupException(
					"接口count必须大于0,method = " + method.getName() + ",\t" + "count = " + count);
		}

		if ((count % QPSEnum.API_METHOD.getMinValue()) != 0) {
			throw new StartupException("接口count必须可以被 " + QPSEnum.API_METHOD.getMinValue() + "整除,method = "
					+ method.getName() + ",\t" + "count = " + count);
		}

		final ZQPSLimitation zQPSLimitation = method.getAnnotation(ZQPSLimitation.class);
		if (zQPSLimitation != null) {
			final ZQPSLimitationEnum type = zQPSLimitation.type();
			if (type == null) {
				throw new IllegalArgumentException(
						"@" + ZQPSLimitation.class.getSimpleName() + ".type 不能为空,method = " + method.getName());
			}
			final int countL = zQPSLimitation.count();
			if (countL <= 0) {
				throw new IllegalArgumentException(
						"@" + ZQPSLimitation.class.getSimpleName() + ".count 必须大于0,method = " + method.getName());
			}

			if (countL < QPSEnum.SERVER.getMinValue()) {
				throw new IllegalArgumentException("@" + ZQPSLimitation.class.getSimpleName() + ".count 不能小于"
						+ QPSEnum.SERVER.getMinValue() + ",method = " + method.getName());
			}

			if (countL > count) {
				throw new IllegalArgumentException(
						zcObject.getClass().getCanonicalName() + "." + method.getName()
						+ " 配置错误：" +
						"@" + ZQPSLimitation.class.getSimpleName() + ".count 不能大于 @"
						+ ZRequestMapping.class.getSimpleName() + ".count"
						);
			}

		}
	}


	public static Object getObjectByMethod(final Method method) {
		return objectMap.get(method);
	}

	public static ZRMethod getMethodByMethodEnumAndPath(final String methodName, final String path) {


		final ZRMethod method = methodPathTable.get(methodName, path);

		if (method != null) {
			return method;
		}

		final Set<String> keySet = methodPathTable.row(methodName).keySet();
		// FIXME 2025年1月22日 下午3:21:16 zhangzhen : 访问 @ZPV的接口值，jp分析getx方法耗时比较长
		final String pathM = getx(path, keySet);
		if (STU.isEmpty(pathM)) {
			return null;
		}

		final ZRMethod method2 = methodPathTable.get(methodName, pathM);
		return method2;
	}

	private static String getx(final String path, final Set<String> keySet) {

		final String[] s = path.replaceAll("//+", "/").split("/");

		for (final String k : keySet) {
			final String[] a = k.split("/");
			if (a.length != s.length) {
				continue;
			}

			int pipeiM = 0;
			int pipei = 0;
			int empty = 0;

			final ArrayList<Object> valueList = new ArrayList<>();
			for (int i = 0; i < s.length; i++) {
				final String t = s[i];
				if (STU.isEmpty(t)) {
					empty++;
					continue;
				}
				if (a[i].startsWith("{") && a[i].endsWith("}")) {
					pipeiM++;
					valueList.add(t);
					continue;
				}

				if (t.equals(a[i])) {
					pipei++;
				}
			}

			if ((pipei + pipeiM + empty) == s.length) {
				ZPVTL.set(valueList);
				return k;
			}
		}

		return null;
	}

	public static Map<String, ZRMethod> getByMethodEnum(final String methodName) {
		return methodPathTable.row(methodName);
	}

	public static Boolean getIsregexByMethodEnumAndPath(final Method method, final String path) {
		return methodIsregexTable.get(method, path);
	}

	private static void checkAPI(final MethodEnum methodEnum, final String mapping, final Method method,
			final Object object, final ZRequestMapping requestMapping) {
		if (methodEnum == null) {
			throw new IllegalArgumentException(MethodEnum.class.getSimpleName() + " 不能为空");
		}

		if (STU.isEmpty(mapping)) {
			throw new IllegalArgumentException("mapping 不能为空");
		}
		if (!mapping.startsWith("/")) {
			throw new IllegalArgumentException("mapping 必须以/开始");
		}
		if (method == null) {
			throw new IllegalArgumentException("method 不能为空");
		}
		if (object == null) {
			throw new IllegalArgumentException("object 不能为空");
		}

		final String mappingMethod = mapping + "@" + requestMapping.method().getMethod();
		final boolean add = mappingSet.add(mappingMethod);
		if (!add) {
			throw new IllegalArgumentException(
					"接口方法的 mapping和Method重复, mapping = " + mapping + "\t" + " method = " + method.getName());
		}
	}

}
