package vo.vortex.http;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import vo.vortex.anno.ZQPSLimitation;
import vo.vortex.anno.ZRequestMapping;
import vo.vortex.cache.ZRC;
import vo.vortex.common.STU;
import vo.vortex.common.ZHashBasedTable;
import vo.vortex.enums.CTEnum;
import vo.vortex.enums.MethodEnum;
import vo.vortex.enums.QPSEnum;
import vo.vortex.enums.ZQPSLimitationEnum;
import vo.vortex.exception.StartupException;
import vo.vortex.http.request.ZMultipartFile;

/**
 * 存取接口方法
 *
 * @author zhangzhen
 * @date 2023年6月28日
 *
 */
public class ZControllerMap {
	static final ZHashBasedTable<ByteArrayKeyWrapper, ByteArrayKeyWrapper, ZRMethod> methodPathTable = new ZHashBasedTable<>();
	static final ZHashBasedTable<Method, ByteArrayKeyWrapper, Boolean> methodIsregexTable = new ZHashBasedTable<>();
	static final Map<Method, Object> objectMap = new HashMap<>(16, 1F);
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

		final ByteArrayKeyWrapper methodWrapper = new ByteArrayKeyWrapper(methodEnum.name().getBytes());
		final ByteArrayKeyWrapper mappingWrapper = new ByteArrayKeyWrapper(mapping.getBytes());

		methodPathTable.put(methodWrapper, mappingWrapper, new ZRMethod(method, cte, zcObject));

		methodIsregexTable.put(method, mappingWrapper, isRegex);

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

	public static ZRMethod getMethodByMethodEnumAndPath(final byte[] methodNameBytes, final byte[] pathBytes) {


		final ByteArrayKeyWrapper methodNameWrapper = new ByteArrayKeyWrapper(methodNameBytes);
		final ByteArrayKeyWrapper pathWrapper = new ByteArrayKeyWrapper(pathBytes);

		final ZRMethod method = methodPathTable.get(methodNameWrapper, pathWrapper);

		if (method != null) {
			return method;
		}

		final Set<ByteArrayKeyWrapper> keySet = methodPathTable.row(methodNameWrapper).keySet();
		// FIXME 2025年1月22日 下午3:21:16 zhangzhen : 访问 @ZPV的接口值，jp分析getx方法耗时比较长
		final Object[] r = getxCache(new String(pathBytes), keySet);
		if (r == null) {
			return null;
		}

		final ZRMethod zrMethod = methodPathTable.get(methodNameWrapper, (ByteArrayKeyWrapper) r[0]);
		zrMethod.setSp((SP) r[1]);

		return zrMethod;
	}

	private static Object[] getxCache(final String path, final Set<ByteArrayKeyWrapper> keySet) {
		final Supplier<SP> getxSupplier = getxSupplier(path, keySet);
		final SP sp = ZRC.singleton().computeIfAbsent(path, getxSupplier, true);
		if (sp == null) {
			return null;
		}

		return new Object[] { sp.getKeyWrapper(), sp };
	}

	private static Supplier<SP> getxSupplier(final String path, final Set<ByteArrayKeyWrapper> keySet) {
		final Supplier<SP> getxSupplier = () -> {
			final String[] s = path.replaceAll("//+", "/").split("/");

			for (final ByteArrayKeyWrapper kw : keySet) {
				final String k = new String(kw.getBytes());
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
//					ZPVTL.set(valueList);
					return new SP(kw, valueList);
				}
			}

			return null;
		};

		return getxSupplier;
	}

	public static Map<ByteArrayKeyWrapper, ZRMethod> getByMethodEnum(final byte[] methodNameBytes) {
		return methodPathTable.row(new ByteArrayKeyWrapper(methodNameBytes));
	}

	public static boolean getIsregexByMethodEnumAndPath(final Method method, final ByteArrayKeyWrapper pathKW) {
		return methodIsregexTable.get(method, pathKW);
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

	public static Map<Method, Object> getMCMap() {
		return objectMap;
	}

}
