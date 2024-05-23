package com.vo.http;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vo.core.QPSEnum;
import com.vo.core.ZContext;
import com.vo.enums.MethodEnum;
import com.vo.exception.StartupException;

import cn.hutool.core.util.StrUtil;

/**
 * 存取接口方法
 *
 * @author zhangzhen
 * @date 2023年6月28日
 *
 */
public class ZControllerMap {
	static final HashBasedTable<MethodEnum, String, Method> methodPathTable = HashBasedTable.create();
	static final HashBasedTable<String, String, Integer> methodQPSTable = HashBasedTable.create();
	static final HashBasedTable<String, String, ZQPSLimitation> methodZQPSLimitationTable = HashBasedTable.create();
	static final HashBasedTable<Method, String, Boolean> methodIsregexTable = HashBasedTable.create();
	static final HashMap<Method, Object> objectMap = Maps.newHashMap();
	static final HashSet<String> mappingSet = Sets.newHashSet();

	/**
	 * 注册一个接口
	 *
	 * @param methodEnum 接口请求方法，如： MethodEnum.POST
	 * @param mapping    匹配路径，如：/index
	 * @param method     具体的接口方法
	 * @param object     接口方法所在的对象
	 * @param isRegex    mapping 是否正则表达式
	 */
	public synchronized static void put(final MethodEnum methodEnum, final String mapping, final Method method,
			final Object object, final boolean isRegex) {

		final ZRequestMapping requestMapping = method.getAnnotation(ZRequestMapping.class);

		checkAPI(methodEnum, mapping, method, object, requestMapping);

		methodPathTable.put(methodEnum, mapping, method);

		methodIsregexTable.put(method, mapping, isRegex);

		objectMap.put(method, object);

		final ZRequestMappingConfigurationProperties zrmConf = ZContext.getBean(ZRequestMappingConfigurationProperties.class);
		final int qps = requestMapping.qps() == ZRequestMapping.DEFAULT_QPS ? zrmConf.getQps() : requestMapping.qps();
		if (qps <= 0) {
			throw new StartupException(
					"接口qps必须大于0,method = " + method.getName() + ",\t" + "qps = " + qps);
		}

		if (qps % QPSEnum.API_METHOD.getMinValue() != 0) {
			throw new StartupException("接口qps必须可以被 " + QPSEnum.API_METHOD.getMinValue() + "整除,method = "
					+ method.getName() + ",\t" + "qps = " + qps);
		}

		methodQPSTable.put(object.getClass().getCanonicalName(), method.getName(), qps);

		final ZQPSLimitation zqpsl = method.getAnnotation(ZQPSLimitation.class);
		if (zqpsl != null) {
			final ZQPSLimitationEnum type = zqpsl.type();
			if (type == null) {
				throw new IllegalArgumentException(
						"@" + ZQPSLimitation.class.getSimpleName() + ".type 不能为空,method = " + method.getName());
			}
			final int qpsL = zqpsl.qps();
			if (qpsL <= 0) {
				throw new IllegalArgumentException(
						"@" + ZQPSLimitation.class.getSimpleName() + ".qps 必须大于0,method = " + method.getName());
			}

			if (qpsL < QPSEnum.SERVER.getMinValue()) {
				throw new IllegalArgumentException("@" + ZQPSLimitation.class.getSimpleName() + ".qps 不能小于"
						+ QPSEnum.SERVER.getMinValue() + ",method = " + method.getName());
			}

			if (qpsL > qps) {
				throw new IllegalArgumentException("@" + ZQPSLimitation.class.getSimpleName() + ".qps 不能大于 @"
						+ ZRequestMapping.class.getSimpleName() + ".qps" + ",method = " + method.getName());
			}

			methodZQPSLimitationTable.put(object.getClass().getCanonicalName(), method.getName(), zqpsl);
		}
	}

	public static ZQPSLimitation getZQPSLimitationByControllerNameAndMethodName(final String controllerName,final String methodName) {
		final ZQPSLimitation zqpsLimitation = methodZQPSLimitationTable.get(controllerName, methodName);
		return zqpsLimitation;
	}

	public static Integer getQPSByControllerNameAndMethodName(final String controllerName,final String methodName) {
		final Integer qps = methodQPSTable.get(controllerName, methodName);
		return qps;
	}

	public static Object getObjectByMethod(final Method method) {
		final Object object = objectMap.get(method);
		return object;
	}


	public static Method getMethodByMethodEnumAndPath(final MethodEnum methodEnum, final String path) {
		final Method method = methodPathTable.get(methodEnum, path);

		if (method != null) {
			return method;
		}

		final Set<String> keySet = methodPathTable.row(methodEnum).keySet();
		final String pathM = getx(path, keySet);
		if (StrUtil.isEmpty(pathM)) {
			return null;
		}

		final Method method2 = methodPathTable.get(methodEnum, pathM);
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

			final ArrayList<Object> list = Lists.newArrayList();
			for (int i = 0; i < s.length; i++) {
				final String t = s[i];
				if (StrUtil.isEmpty(t)) {
					empty++;
					continue;
				}
				if (a[i].startsWith("{") && a[i].endsWith("}")) {
					pipeiM++;
					list.add(t);
					continue;
				}

				if (t.equals(a[i])) {
					pipei++;
				}
			}

			if (pipei + pipeiM + empty == s.length) {
				ZPVTL.set(list);
				return k;
			}
		}

		return null;
	}

	public static Map<MethodEnum, Method> getByPath(final String path) {
		final Map<MethodEnum, Method> column = methodPathTable.column(path);
		return column;
	}
	public static Map<String, Method> getByMethodEnum(final MethodEnum methodEnum) {

		final Map<String, Method> row = methodPathTable.row(methodEnum);
		return row;
	}

	public static Boolean getIsregexByMethodEnumAndPath(final Method method, final String path) {
		return methodIsregexTable.get(method, path);
	}

	private static void checkAPI(final MethodEnum methodEnum, final String mapping, final Method method,
			final Object object, final ZRequestMapping requestMapping) {
		if (methodEnum == null) {
			throw new IllegalArgumentException(MethodEnum.class.getSimpleName() + " 不能为空");
		}

		if (StrUtil.isEmpty(mapping)) {
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
