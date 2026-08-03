package vo.vortex.scanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import vo.log.core.ZLog2;
import vo.vortex.anno.ZController;
import vo.vortex.anno.ZCookieValue;
import vo.vortex.anno.ZPathVariable;
import vo.vortex.anno.ZRequestMapping;
import vo.vortex.anno.ZRestController;
import vo.vortex.api.StaticController;
import vo.vortex.bean.ZObjectGeneratorStarter;
import vo.vortex.bean.ZSingleton;
import vo.vortex.common.AU;
import vo.vortex.common.STU;
import vo.vortex.configuration.properties.ServerConfigurationProperties;
import vo.vortex.core.ZApplicationStartupInfo;
import vo.vortex.core.ZContext;
import vo.vortex.enums.BeanModeEnum;
import vo.vortex.enums.CTEnum;
import vo.vortex.enums.MethodEnum;
import vo.vortex.exception.StartupException;
import vo.vortex.g.APT;
import vo.vortex.g.G;
import vo.vortex.http.Task;
import vo.vortex.http.ZControllerMap;
import vo.vortex.http.ZCookie;
import vo.vortex.http.request.ZMultipartFile;
import vo.vortex.http.response.ZResponse;

/**
 * 扫描 @ZController 的类，注册为一个控制类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
// FIXME 2024年12月21日 下午3:23:26 zhangzhen : TODO：考虑这个问题：
//	上传文件尤其是大文件并且带一个认证用的header(比如谷歌验证码等)的情况
// 按现在的逻辑是 读取完了文件并且解析出全部的表单字段以后才开始执行目标业务方法，才开始校验header
// 这时候如果手误传错了header甚至是恶意的频繁form-data请求，就会验证浪费服务器性能和存储空间。怎么办？
// 1、现在能想到的是：读取body时边读边解析出表单字段，但是同时也要保存好file的内容，和在业务方法中校验header并无本质区别。
public class ZControllerScanner {


	private static final ZLog2 LOG = ZLog2.getInstance();

	/**
	 * API允许的 ZMultipartFile 参数的个数
	 */
	private static final int ZMF_SIZE = 1;

	private static final HashSet<Class<? extends Annotation>> HTTP_METHOD_SET = new HashSet<>();

	static {
		HTTP_METHOD_SET.add(ZRequestMapping.class);
	}

	public static Set<Class<?>> scanAndCreateObject(final ZApplicationStartupInfo startupInfo) {
		//		ZControllerScanner.LOG.info("开始扫描带有[{}]的类", ZController.class.getCanonicalName());

		final Set<Class<?>> load = G.getAllClass();
//		System.out.println("ZControllerScanner.G.getAllClass.size = " + load.size());

		final Set<Class<?>> x = load.stream()
				.filter(cls -> cls!=null)
				.filter(cls -> cls.isAnnotationPresent(ZRestController.class)
						|| cls.isAnnotationPresent(ZController.class))
				.collect(Collectors.toSet());

		final Set<Class<?>> clsSet = new HashSet<>(x);

		clsSet.addAll(APT.getAllClass().stream()
				.filter(cls -> cls!=null)
				.filter(cls -> cls.isAnnotationPresent(ZRestController.class)
						|| cls.isAnnotationPresent(ZController.class))
				.collect(Collectors.toSet()));

		final Set<Class<?>> restControllerSet = clsSet.stream()
				.filter(c -> c.isAnnotationPresent(ZRestController.class)).collect(Collectors.toSet());
		final Set<Class<?>> controllerSet = clsSet.stream()
				.filter(c -> c.isAnnotationPresent(ZController.class)).collect(Collectors.toSet());
//		//		ZControllerScanner.LOG.info("带有[{}]的类个数={}", ZController.class.getCanonicalName(), zcSet.size());

		// 1
//		final Set<Class<?>> restControllerSet = ClassMap.scanPackageByAnnotation(ZRestController.class, startupInfo.getPackageNameArray());
//		final Set<Class<?>> controllerSet = ClassMap.scanPackageByAnnotation(ZController.class, startupInfo.getPackageNameArray());

		final Set<Class<?>> rcR = new HashSet<>(restControllerSet);
		final Set<Class<?>> cR = new HashSet<>(controllerSet);

		rcR.retainAll(cR);
		if (rcR.size() > 0) {
			final String cn = rcR.stream().map(Class::getCanonicalName).collect(Collectors.joining(","));
			throw new StartupException(
					"不允许 @" + ZRestController.class.getName()
					+ " 和 @" + ZController.class.getName()
					+ " 同时使用,class = " + cn
			);
		}

		final ServerConfigurationProperties serverConfiguration = ZContext.getBean(ServerConfigurationProperties.class);


		final Set<Class<?>> zcSet = new HashSet<>(restControllerSet);
		zcSet.addAll(controllerSet);

		for (final Class<?> cls : zcSet) {
			final boolean staticControllerEnable = serverConfiguration.getStaticControllerEnable();
			if (!staticControllerEnable
					&& cls.getName().equals(StaticController.class.getName())) {

				//				ZControllerScanner.LOG.info("[{}] 未启用，不创建[{}]对象", StaticController.class.getSimpleName(),
				//						StaticController.class.getSimpleName());

				continue;
			}

			final Object newZController1 = ZObjectGeneratorStarter.generate(cls);

			//			LOG.info("带有[{}]的类[{}]创建对象[{}]完成", ZController.class.getCanonicalName(), cls.getCanonicalName(),
			//					newZController1);

			ZContext.addBean(cls, newZController1);

			final Object controllerObject = ZControllerScanner.getSingleton(cls);

			final ZRestController restController = cls.getAnnotation(ZRestController.class);
			final ZController controller = cls.getAnnotation(ZController.class);
			final String prefix = checkCPrefix(
					restController != null ? restController.prefix() : controller.prefix());


			final Method[] ms = cls.getDeclaredMethods();

			Arrays.stream(ms)
			.parallel()
			.forEach(method ->{


				// 校验 @ZRequestMapping
				final ZRequestMapping requestMappingAnnotation = method.getAnnotation(ZRequestMapping.class);
				if (requestMappingAnnotation == null) {
					return;
				}

				checkNoVoidWithZResponse(cls, method);

				final String[] requestMappingArray = requestMappingAnnotation.mapping();

				ZControllerScanner.checkZRequestMapping(method, requestMappingAnnotation, requestMappingArray);

				final boolean[] isRegex = requestMappingAnnotation.isRegex();
				final MethodEnum methodEnum = requestMappingAnnotation.method();
				for (int i = 0; i < requestMappingArray.length; i++) {
					final String mapping = requestMappingArray[i];

					ZControllerMap.put(methodEnum, prefix + mapping, method,
							restController!=null ? CTEnum.REST : CTEnum.NORMAL
							, controllerObject, isRegex[i]);
				}

				checkZMFIleSize(cls, method);

			});
		}

		return zcSet;
	}

	private static void checkZMFIleSize(final Class<?> cls, final Method method) {
		final int parameterCount = method.getParameterCount();
		if (parameterCount > 0) {
			int zmfC = 0;
			final Parameter[] ps = method.getParameters();
			for (final Parameter element : ps) {
				if (ZMultipartFile.class.equals(element.getType())) {
					zmfC++;
					if (zmfC > ZMF_SIZE) {
						final String message = "接口方法[" + cls.getSimpleName() + "." + method.getName() + "]最多允许有[" + ZMF_SIZE
								+ "]个[" + ZMultipartFile.class.getSimpleName() + "]参数,当前已找到[" + zmfC + "]个";
						throw new StartupException(message);
					}
				}
			}
		}
	}

	/**
	 * 简单判断一下prefix 以/开头不以/结尾
	 * TODO 使用正则表达式来判断
	 *
	 * @param prefix
	 * @return
	 *
	 */
	private static String checkCPrefix(final String prefix) {
		if (STU.isEmpty(prefix)) {
			return "";
		}

		if (!prefix.equals(prefix.trim())) {
			throw new StartupException("@" + ZRestController.class.getSimpleName() + ".prefix" + " 不能是blank");
		}

		if (prefix.charAt(0) != '/') {
			throw new StartupException("@" + ZRestController.class.getSimpleName() + ".prefix" + " 必须以/开头");
		}

		if (prefix.charAt(prefix.length() - 1) == '/') {
			throw new StartupException("@" + ZRestController.class.getSimpleName() + ".prefix" + " 不能以/结尾");
		}

		return prefix;
	}

	private static void checkNoVoidWithZResponse(final Class cls, final Method method) {
		if (!Task.VOID.equals(method.getReturnType().getCanonicalName())) {
			final Parameter[] ps = method.getParameters();

			final Optional<Parameter> ro =
					Arrays.stream(ps)
					.filter(p -> p.getType().getCanonicalName().equals(ZResponse.class.getCanonicalName()))
					.findAny();
			if (ro.isPresent()) {
				throw new StartupException(
						"接口方法 " + cls.getCanonicalName() + "." + method.getName() + " 带返回值不允许使用 " + ZResponse.class.getSimpleName() + " 参数，去掉 "
								+ ZResponse.class.getSimpleName() + " 参数，或者返回值改为 void");
			}
		} else {
			final Parameter[] ps = method.getParameters();

			final Optional<Parameter> ro =
					Arrays.stream(ps)
					.filter(p -> p.getType().getCanonicalName().equals(ZResponse.class.getCanonicalName()))
					.findAny();
			if (!ro.isPresent()) {
				throw new StartupException(
						"接口方法 " + cls.getCanonicalName() + "." + method.getName() + " 无返回值，必须加入 " + ZResponse.class.getSimpleName() + " 参数，加入 "
								+ ZResponse.class.getSimpleName() + " 参数，或者返回值改为非void");
			}
		}
	}

	private static void checkZRequestMapping(final Method method, final ZRequestMapping requestMappingAnnotation,
			final String[] requestMappingArray) {

		if (AU.isEmpty(requestMappingArray)) {
			throw new StartupException("接口方法 " + method.getName() + " mapping值不能为空");
		}

		final Parameter[] ps = method.getParameters();

		// @ZCookieValue 校验
		final Optional<Parameter> zcvO = Arrays.stream(ps).filter(p -> p.isAnnotationPresent(ZCookieValue.class))
				.findAny();
		if (zcvO.isPresent()) {
			final Class<?> type = zcvO.get().getType();
			final ZCookieValue cookieValue = zcvO.get().getAnnotation(ZCookieValue.class);
			if (type.equals(String.class)) {
				if (STU.isEmpty(cookieValue.name())) {
					throw new StartupException("接口方法[" + method.getName() + "]的@" + ZCookieValue.class.getSimpleName()
							+ " 参数用于 String 类型时，" + zcvO.get().getName() + " 名称表示的是 Cookie的value，所以"
							+ "请声明@" + ZCookieValue.class.getSimpleName() + ".name() 属性来表示Cookie的name");
				}

			} else if (type.equals(ZCookie.class)) {
				if (STU.isEmpty(cookieValue.name())) {
					throw new StartupException("接口方法[" + method.getName() + "]的@" + ZCookieValue.class.getSimpleName()
							+ " 参数用于 "+ZCookie.class.getSimpleName()+" 类型时，" + zcvO.get().getName() + " 名称表示的是 "+ZCookie.class.getSimpleName()+"对象，所以"
							+ "请声明@" + ZCookieValue.class.getSimpleName() + ".name() 属性来表示Cookie的name");
				}
			} else {
				throw new StartupException("接口方法[" + method.getName() + "] @" + ZCookieValue.class.getSimpleName()
						+ " 注解只能用于 String 或 " + ZCookie.class.getSimpleName()
						+ " 类型，当前用于 " + type.getSimpleName() + " 类型");
			}
		}

		// requestMappingArray 如果有 @ZPathVariable，则长度只能为1
		for (final String mapping : requestMappingArray) {
			final String p1 = mapping.replaceAll("//+", "/");
			if (!mapping.equals(p1)) {
				throw new StartupException(
						"接口方法mapping 必须使用一个/分隔,接口方法=" + method.getName() + ",mapping=" + mapping);
			}

			final String[] sa = p1.split("/");
			final List<String> zpvNameList = new ArrayList<>();
			for (final String s : sa) {
				if (s.length() <= 1) {
					continue;
				}
				if (((s.charAt(0) == '{') && (s.charAt(s.length() - 1) == '}'))) {
					zpvNameList.add(s.substring(1, s.length() - 1));
					if ((requestMappingArray.length > 1)) {
						throw new StartupException(
								"接口方法[" + method.getName() + "]参数使用@" + ZPathVariable.class.getSimpleName()
								+ ",则mapping只能声明一个,当前声明了" + requestMappingArray.length + "个"
								+ Arrays.toString(requestMappingArray) + ",请修改");
					}
				}
			}

			final List<Parameter> zpvPList =
					Arrays.stream(ps)
					.filter(p -> p.isAnnotationPresent(ZPathVariable.class)).collect(Collectors.toList());

			if (zpvPList.size() != zpvNameList.size()) {
				throw new StartupException("接口方法mapping 声明@" + ZPathVariable.class.getSimpleName() + "个数["
						+ zpvNameList.size() + "]与方法参数个数[" + zpvPList.size() + "]不一致,接口方法=" + method.getName()
						+ ",mapping=" + mapping);
			}

			if (!zpvPList.isEmpty()) {
				for (int i = 0; i < zpvPList.size(); i++) {
					// 可以按名称顺序判断，因为一个方法中参数名称不可以重复
					if (!zpvPList.get(i).getName().equals(zpvNameList.get(i))) {
						final List<String> pnl = zpvPList.stream().map(Parameter::getName)
								.collect(Collectors.toList());
						throw new StartupException("接口方法mapping 声明@" + ZPathVariable.class.getSimpleName()
								+ "参数顺序" + pnl + "与方法mapping" + zpvNameList + "顺序不一致,请修改参数顺序。接口方法=" + method.getName()
								+ ",mapping=" + mapping);
					}
				}

				for (final Parameter p : zpvPList) {
					final String canonicalName = p.getType().getCanonicalName();

					if (!Task.ZPV_TYPE.contains(canonicalName)) {
						throw new StartupException("接口方法["+method.getName()+"]的@" + ZPathVariable.class.getSimpleName() + "参数类型["
								+ canonicalName + "]不支持，支持类型为" + Task.ZPV_TYPE);
					}
				}

			}
		}

		final boolean[] isRegex = requestMappingAnnotation.isRegex();
		if (!AU.isEmpty(isRegex) && (isRegex.length != requestMappingArray.length)) {
			throw new StartupException("接口方法 " + method.getName() + " isRegex个数必须与mapping值个数 相匹配, isRegex个数 = "
					+ isRegex.length + " mapping个数 = " + requestMappingArray.length);
		}

		final Set<String> temp = new HashSet<>();

		for (final String requestMapping : requestMappingArray) {

			if (STU.isEmpty(requestMapping)) {
				throw new StartupException("接口方法 " + method.getName() + " mapping值不能为空");
			}

			ZControllerScanner.checkRequestMapping(method, requestMapping);

			final boolean add = temp.add(requestMapping + "@" + requestMappingAnnotation.method().getMethod());
			if (!add) {
				throw new StartupException(
						"接口方法 " + method.getName() + " mapping值不能重复,mapping = " + Arrays.toString(requestMappingArray));
			}
		}

	}

	/**
	 * 校验requestMapping,必须以且只以一个/开头
	 *
	 * @param method
	 * @param requestMapping
	 *
	 */
	private static void checkRequestMapping(final Method method, final String requestMapping) {
		if (requestMapping.charAt(0) != '/') {
			throw new StartupException("接口方法 " + method.getName() + " mapping值必须以/开始,method = "
					+ method.getName() + " requestMapping = " + requestMapping);
		}

		if (requestMapping.length() <= 1) {
			return;
		}

		final char charAt = requestMapping.charAt(1);
		if (charAt == '/') {
			throw new StartupException("接口方法 " + method.getName() + " mapping值必须以/开始,method = "
					+ method.getName() + " requestMapping = " + requestMapping);
		}

	}

	private static boolean isHttpMethod(final Method method) {
		for (final Class<? extends Annotation> c : ZControllerScanner.HTTP_METHOD_SET) {
			if (method.isAnnotationPresent(c)) {
				return true;
			}
		}

		return false;
	}

	private static Object getSingleton(final Class<?> zcClass) {
		final ZRestController zc = zcClass.getAnnotation(ZRestController.class);
		if (zc != null) {

			final BeanModeEnum modeEnum = zc.modeEnum();

			switch (modeEnum) {
			case SINGLETON:
				final Object singletonByClass = ZSingleton.getSingletonByClass(zcClass);
				return singletonByClass;

			default:
				break;
			}
		}

		final ZController c = zcClass.getAnnotation(ZController.class);
		if (c != null) {

			final BeanModeEnum modeEnum = c.modeEnum();

			switch (modeEnum) {
			case SINGLETON:
				final Object singletonByClass = ZSingleton.getSingletonByClass(zcClass);
				return singletonByClass;

			default:
				break;
			}
		}

		return null;
	}
}
