package vo.zframework.http;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import vo.zframework.anno.ZCookieValue;
import vo.zframework.anno.ZMax;
import vo.zframework.anno.ZMin;
import vo.zframework.anno.ZPathVariable;
import vo.zframework.anno.ZPositive;
import vo.zframework.anno.ZQPSLimitation;
import vo.zframework.anno.ZRequestBody;
import vo.zframework.anno.ZRequestHeader;
import vo.zframework.anno.ZRequestMapping;
import vo.zframework.aop.InterceptorParameter;
import vo.zframework.cache.ZRC;
import vo.zframework.common.AU;
import vo.zframework.common.CU;
import vo.zframework.common.J;
import vo.zframework.common.RU;
import vo.zframework.common.STU;
import vo.zframework.configuration.properties.RequestValidatorConfigurationProperties;
import vo.zframework.configuration.properties.ServerConfigurationProperties;
import vo.zframework.core.ZContext;
import vo.zframework.enums.CTEnum;
import vo.zframework.enums.ContentTypeEnum;
import vo.zframework.enums.HttpStatusEnum;
import vo.zframework.enums.MethodEnum;
import vo.zframework.enums.QPSHandlingEnum;
import vo.zframework.exception.FormPairParseException;
import vo.zframework.exception.ParsingRequestParamException;
import vo.zframework.exception.PathVariableException;
import vo.zframework.html.ResourcesLoader;
import vo.zframework.http.request.FormData;
import vo.zframework.http.request.HttpRequestParser;
import vo.zframework.http.request.HttpRequestProcessor;
import vo.zframework.http.request.RequestParam;
import vo.zframework.http.request.ZMultipartFile;
import vo.zframework.http.request.ZRequest;
import vo.zframework.http.request.ZRequestParam;
import vo.zframework.http.response.ReU;
import vo.zframework.http.response.ZResponse;
import vo.zframework.http.response.ZResponseStatus;
import vo.zframework.scanner.ZHandlerInterceptor;
import vo.zframework.scanner.ZHandlerInterceptorScanner;
import vo.zframework.scanner.ZModelAndView;
import vo.zframework.template.ZModel;
import vo.zframework.template.ZTemplateEngine;
import vo.zframework.validator.ZValidated;
import vo.zframework.validator.ZValidator;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年7月3日
 *
 */
public class Task {

	private static final Class<ZResponse> ZRESPONSE_CLASS = ZResponse.class;
	private static final Object[] EMPTY_OBJECT_ARRAY = {};
	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES = ZContext
			.getBean(ServerConfigurationProperties.class);
	private static final RequestValidatorConfigurationProperties REQUEST_VALIDATOR_CONFIGURATION_PROPERTIES = ZContext.getBean(RequestValidatorConfigurationProperties.class);

	/**
	 * 避免html.getBytes而加的缓存，只在内容变化不频繁或者纯粹的静态html页面时，此缓存才有效果
	 */
	private static final ZRC HTML_BYTES_ZRC = new ZRC(20, 1);

	public static final String DEFAULT_CHARSET_NAME = Charset.defaultCharset().displayName();
	public static final String VOID = "void";
	public static final ContentTypeEnum DEFAULT_CONTENT_TYPE = ContentTypeEnum.APPLICATION_JSON;

	/**
	 * 执行目标方法（接口Method）
	 *
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public static ZResponse invoke(final ZRequest request) throws Throwable {

		try {

			final ZRMethod zrMethod = ZConnectionTL.get().getPd().getZrMethod();
			final Object[] parameters = generateParameters(request, request.getPath(), zrMethod);

			final Object zController = ZControllerMap.getObjectByMethod(zrMethod.getMethod());
			return invokeAndResponse(zrMethod, parameters, zController, request);

		} catch (final Throwable e) {
			//			e.printStackTrace();
			// 这里不处理，抛出去
			throw e;
		}

	}

	/**
	 * 使用 server.method的配置值中非参数 methodEnum的选项 和 URI来匹配目标接口Method
	 * @param methodNameBytes
	 * @param path
	 * @param pathBytes TODO
	 * @return
	 */
	public static ZRMethod matchWithServerMethod(final byte[] methodNameBytes, final String path, final byte[] pathBytes) {

		final MethodEnum[] es = MethodEnum.values();
		for (final MethodEnum methodEnum : es) {
			final byte[] meNameBytes = methodEnum.getMethodBytes();
			final boolean methodSupportBytes = HttpRequestProcessor.methodSupportBytes(meNameBytes);
			if (methodSupportBytes && !Arrays.equals(meNameBytes, methodNameBytes)) {
				final ZRMethod methodT = ZControllerMap.getMethodByMethodEnumAndPath(meNameBytes, pathBytes);
				if (methodT != null) {
					return methodT;
				}
			}
		}

		return null;
	}

	public static ZRMethod getMatcheMethodCache(final byte[] methodNameBytes, final String path) {

		final Supplier<ZRMethod> supplier = () -> {
			final Map<ByteArrayKeyWrapper, ZRMethod> rowMap = ZControllerMap.getByMethodEnum(methodNameBytes);
			final Set<Entry<ByteArrayKeyWrapper, ZRMethod>> entrySet = rowMap.entrySet();
			for (final Entry<ByteArrayKeyWrapper, ZRMethod> entry : entrySet) {
				final ZRMethod methodTarget = entry.getValue();
				final ByteArrayKeyWrapper requestMappingKW = entry.getKey();
				if (ZControllerMap.getIsregexByMethodEnumAndPath(methodTarget.getMethod(), requestMappingKW)
						&& path.matches(new String(requestMappingKW.getBytes()))) {

					return methodTarget;
				}
			}

			return null;
		};

		final String key = "gmm-" + path;
		return ZRC.singleton().computeIfAbsent(key, supplier, true);
	}

	public static String gExceptionMessage(final Throwable e) {

		if (Objects.isNull(e)) {
			return "";
		}

		final StringWriter stringWriter = new StringWriter();
		final PrintWriter writer = new PrintWriter(stringWriter);
		e.printStackTrace(writer);

		return stringWriter.toString();
	}

	private static ZResponse invokeAndResponse(
					final ZRMethod zrMethod,
					final Object[] parameters,
					final Object zControllerObject,
					final ZRequest request) throws Throwable {

		final ZRequestMapping requestMapping = zrMethod.getZRequestMapping();
		if (requestMapping.qpsLimit()) {
			final ZResponse checkZRequestMappingQPS = checkZRequestMappingCount(request, zControllerObject, zrMethod);
			if (checkZRequestMappingQPS != null) {
				return checkZRequestMappingQPS;
			}
		}

		final ZResponse checkZQPSLimitation = checkZQPSLimitation(zrMethod, request, zControllerObject.getClass().getName());
		if (checkZQPSLimitation != null) {
			return checkZQPSLimitation;
		}

		ZResponseStatus.initialization();

		setZRequestAndZResponse(request, parameters);

		final List<ZHandlerInterceptor> hiList = ZHandlerInterceptorScanner.match(request.getRequestURI());

		final Object r = CU.isEmpty(hiList)
				? invoke0(zControllerObject, zrMethod, parameters)
				: invokeZHandlerInterceptor(zrMethod, parameters, zControllerObject, request, hiList);

		// 最高优先级：业务代码处理 接口方法void
		// 1、先看方法里的业务代码是否new ZResponse.write过了，有则停止，无则继续第二步
		// 2、用接口的ZResponse参数来contentType然后write，有次参数并且设置了ct则直接write，无则第3步
		// 3、2有ZR参数但未CT，则设为produces然后write。
		//    2无ZR，则给一个默认的json 200
		// 	到此结束了，不管produces是啥都write
		if (zrMethod.isVoid()) {
			final boolean written = ZResponseStatus.isWritten();
			if (written) {
				// 已write了，业务代码自己处理过了，停止
				return null;
			}

			final ZResponse response = ZHttpContext.getZResponseAndRemove();
			// 无ZR参数，直接给一个默认的json 200
			if (response == null) {
				return new ZResponse()
						.contentType(ContentTypeEnum.APPLICATION_JSON.getTypeBytes());
			}

			// 有ZR参数未CT，根据produces然后看类的注解
			final String contentType = response.getContentType();
			if (contentType == null) {
				final String p1 = findProduces(request, zrMethod.getProduces());
				response.contentType(p1 == null ? DEFAULT_CONTENT_TYPE.getType() : p1);
				// FIXME 2025年12月6日 00:08:25 zhangzhen :  逻辑似乎不对
				// 到此应该在代码里已经设置了body了（如果有body），那么在body后设置CT已经无意义了
				// 应该在invoke前先匹配好先设置CT，在body时根据CT来选择不同的CT格式
				// 或者直接简单点?框架只管CT，body格式让用户自己设置？
//				response.bo
			}

			// 到此，有ZR参数且CT了，直接返回
			return response;
		}

		// 第二优先：produces 设定
		// 只设定了一个则就按这个，设置多个则选择匹配度最高的，都不匹配则按顺序返回第一个
		final String[] ps = zrMethod.getProduces();
		if (AU.isNotEmpty(ps)) {
			if (ps.length == 1) {
				return responseCT(r, ps[0], zrMethod.getCtea()[0]);
			}
			final int x = 20;
			// FIXME 2025年12月6日 00:39:34 zhangzhen : 多个ps的待会再做，先做下面简单的

		}

		// 第三优先：@ZResponseBody 注解，返回类型String则响应text/plain
		// 否则一律application/json
		if (zrMethod.hasResponseBody()) {
			if (zrMethod.isRTString() || zrMethod.isRTPrimitiveType()) {
				return responseTextPlain(r);
			}
			return responseAppJSON(r);
		}

		// 第4优先：@ZRestCon还是@ZCon注解,ZC则默认为html名称，
		// ZRC则区分returnType为String/基本类型则CT为text/plain，其他一律json
		final CTEnum ctEnum = zrMethod.getCtEnum();
		if (ctEnum == CTEnum.NORMAL) {
			final ZModel zModel = findZModel(parameters);
			return responseHtml(r, zModel);
		}

		if ((ctEnum == CTEnum.REST) && (zrMethod.isRTString() || zrMethod.isRTPrimitiveType())) {
			return responseTextPlain(r);
		}

		// 默认响应json
		return responseAppJSON(r);
	}

	private static ZModel findZModel(final Object[] parameters) {
		for (final Object p : parameters) {
			if (p.getClass() == ZModel.class) {
				return (ZModel) p;
			}
		}
		return null;
	}

	private static ZResponse checkZRequestMappingCount(
			final ZRequest request,
			final Object zControllerObject,
			final ZRMethod zrMethod) {

		final ZRequestMapping requestMapping = zrMethod.getZRequestMapping();

		final QPSHandlingEnum handlingEnum = REQUEST_VALIDATOR_CONFIGURATION_PROPERTIES.getHandlingEnum(request.getUserAgent());

		final String keyPrefix = "a-" + zControllerObject.getClass().getName().hashCode() + '@' + zrMethod.getMethod().getName().hashCode();
		final boolean allow = QC.allow(
			 requestMapping.time(),
						keyPrefix,
				requestMapping.count(),
					handlingEnum);

		if (!allow) {
			// FIXME 2025年1月3日 上午4:19:33 zhangzhen : 这里有个严重的问题会导致可能浪费服务器性能和存储空间
			// 尤其是上传文件尤其是很大的文件时，因为当前逻辑是解析完body并且save到临时文件之后，才会走到
			// 什么的判断api.qps的部分，所以频繁上传可能再次导致不执行api
			// 前几天写的功能[自定义http解析流程]，似乎可以把这个部分逻辑放进去，
			// 即：先解析header如果API.qps超了，则不解析body

			return ReU.response429API();
		}

		return null;
	}

	private static Object invokeZHandlerInterceptor(final ZRMethod zrMethod, final Object[] parameters,
			final Object zControllerObject, final ZRequest request, final List<ZHandlerInterceptor> zhiList) throws Throwable {

		// FIXME 2026年6月10日 09:30:02 zhangzhen : 这里又new ZResponse应该是bug，应该取上面set过的ZResponse对象。
		final ZResponse response = ZHttpContext.getZResponse();
//		final ZResponse response = new ZResponse();

		final InterceptorParameter interceptorParameter =
				new InterceptorParameter(
			zrMethod.getMethod().getName(), zrMethod.getMethod(),
				zrMethod.getMethod().getReturnType().getName().equals(Void.class.getName()),
				zControllerObject, parameters);

		// 1 按从小到大执行preHandle
		boolean stop = false;
		for (int i = 0; i < zhiList.size(); i++) {
			final ZHandlerInterceptor hi = zhiList.get(i);
			final boolean preHandle = hi.preHandle(request, response, interceptorParameter);
			if (!preHandle) {
				stop = true;
				break;
			}
		}

		// 有 preHandle 返回false，直接返回response（在preHandle可能设值了）
		if (stop) {
			return response;
		}

		// 2 执行目标方法
		final Object rV = invoke0(zControllerObject, zrMethod, parameters);

		final ZModel zModel = findZModel(parameters);

		final ZModelAndView modelAndView =
				zrMethod.getCtEnum() == CTEnum.NORMAL
				? new ZModelAndView(true, String.valueOf(rV), readHtmlContent(rV), zModel.getData(),
						(ZModel) Arrays.stream(parameters).filter(arg -> arg.getClass().equals(ZModel.class))
						.findAny().orElse(null),
						null)
						: new ZModelAndView(false, null, null, null, (ZModel) null, rV);

		// 3 按从大到小执行postHandle
		for (int i = zhiList.size() - 1; i >= 0; i--) {
			final ZHandlerInterceptor hi = zhiList.get(i);
			hi.postHandle(request, response, interceptorParameter, modelAndView);
		}

		// 4 按从大到小执行afterCompletion
		for (int i = zhiList.size() - 1; i >= 0; i--) {
			final ZHandlerInterceptor hi = zhiList.get(i);
			hi.afterCompletion(request, response, interceptorParameter, modelAndView);
		}

		return rV;
	}

	private static ZResponse checkZQPSLimitation(
			final ZRMethod zrMethod,
			final ZRequest request,
			final String controllerName) {

		final ZQPSLimitation zqpsLimitation = zrMethod.getZqpsLimitation();
		if (zqpsLimitation == null) {
			return null;
		}

		switch (zqpsLimitation.type()) {

		case ZSESSIONID:
			if (!SERVER_CONFIGURATIONPROPERTIES.isResponseZSessionId()) {
				break;
			}

			final ZSession session = Task.getOrGSession(request);
			final String keyword = gzqpsLimitationKeyword(zrMethod, controllerName, session);

			final QPSHandlingEnum handlingEnum = REQUEST_VALIDATOR_CONFIGURATION_PROPERTIES
					.getHandlingEnum(request.getUserAgent());

			if (!QC.allow(zqpsLimitation.time(), keyword, zqpsLimitation.count(), handlingEnum)) {
				return ReU.response429ZSESSIONID();
			}

			break;

		default:
			break;
		}

		return null;
	}

	private static String gzqpsLimitationKeyword(final ZRMethod zrMethod, final String controllerName,
			final ZSession session) {
		final String keyword = controllerName
				+ "@" + zrMethod.getMethod().getName()
				+ "@ZQPSLimitation" + '_'
				+ session.getId();
		return keyword;
	}

	static String findProduces(final ZRequest request, final String[] ps) {
		if (AU.isEmpty(ps)) {
			return null;
		}

		if (ps.length == 1) {
			// FIXME 2025年12月6日 00:03:18 zhangzhen :  要不要看Accept看是否响应406？
			return ps[0];
		}

		// FIXME 2025年12月6日 00:03:03 zhangzhen :  以后在解析，先做一个的
		final String accept = request.getHeader("Accept");

		return null;
	}

	/**
	 * 优先从request中获取ZSESSIONID，如果服务器中不存在，则生成新的并Set-Cookie
	 *
	 * @param request
	 * @return
	 */
	private static ZSession getOrGSession(final ZRequest request) {
		return request.getSession(true);
//		final ZSession sessionFAlSE = request.getSession(false);
//		if (sessionFAlSE != null) {
//			return sessionFAlSE;
//		}
//
//		return request.getSession(true);
	}

	/**
	 * 真正的API目标方法执行，统一在本方法里面执行，方便统一处理
	 *
	 * @param zControllerObject		此method所在的 @ZController 标记的对象
	 * @param zrMethod 				组合的Method相关内容的对象
	 * @param parameters			此method的参数数组，如：ZRequest/ZModel/@ZRequestHeader/@ZRequestParam等等
	 * @return
	 * @throws Throwable
	 */
	private static Object invoke0(final Object zControllerObject, final ZRMethod zrMethod, final Object[] parameters) throws Throwable {

		try {
			return zrMethod.getMethodHandle().invokeExact(parameters);
		} catch (final Throwable e) {
			throw e;
		} finally {

			if (parameters.length > 0) {
				try {
					closeZMFInputStreamAndDeleteTempFile(parameters);
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private static void closeZMFInputStreamAndDeleteTempFile(final Object[] arraygP) throws IOException {

		for (int i = arraygP.length - 1; i >= 0; i--) {
			final Object p = arraygP[i];
			if (p == null) {
				// 可能是 @ZRequestParam 参数，可能是null
				continue;
			}

			if (ZMultipartFile.class.equals(p.getClass())) {
				final ZMultipartFile file = (ZMultipartFile) p;
				try (final InputStream inputStream2 = file.getInputStream()) {
				}

				final String tempFilePath = file.getTempFilePath();
				if (tempFilePath != null) {
					final File tFile = new File(tempFilePath);
					if (tFile.exists()) {
						tFile.delete();
						tFile.deleteOnExit();
					}
				}

				break;
			}
		}
	}

	private static ZResponse responseCT(final Object r, final String contentType, final ContentTypeEnum cte) {
		final ZResponse rx = new ZResponse().contentType(contentType);
		cte.body(r, rx);
		return rx;
	}

	private static ZResponse responseTextPlain(final Object r) {
		return new ZResponse()
				.contentType(ContentTypeEnum.TEXT_PLAIN.getTypeBytes())
				.body(r instanceof String ? (String) r : String.valueOf(r));
	}

	private static ZResponse responseAppJSON(final Object r) {
		final String json = J.toJSONString(r);
		return new ZResponse().contentType(ContentTypeEnum.APPLICATION_JSON.getTypeBytes()).body(json);
	}

	private static ZResponse responseHtml(final Object r, final ZModel zModel) {
		try {

			final String htmlContent = readHtmlContent(r);

			final ZTemplateEngine templateEngine = ZContext.getBean(ZTemplateEngine.class);
			if (templateEngine == null) {
				throw new IllegalArgumentException("无" + ZTemplateEngine.class.getSimpleName() + "，请配置或添加starter");
			}

			final String html = templateEngine.render(String.valueOf(r), htmlContent, zModel);

			final byte[] htmlBytes = HTML_BYTES_ZRC.computeIfAbsent(html, () -> html.getBytes());

			return new ZResponse().contentType(ContentTypeEnum.TEXT_HTML.getTypeBytes()).body(htmlBytes);

		} catch (final Exception e) {
			throw e;
		}
	}

	private static String readHtmlContent(final Object r) {
		final String ss = String.valueOf(r);
		final String htmlName = ss.charAt(0) == '/' ? ss : '/' + ss;
		final String htmlContent = ResourcesLoader.loadStaticResourceString(htmlName);
		return htmlContent;
	}

	private static Object[] generateParameters0(
			final ZRequest request,
			final String path,
			final ZRMethod zrMethod)
					throws Exception {

		final Object[] parameters = new Object[zrMethod.getMethodParameterSize()];

		int pI = 0;
		int zpvPI = 0;

		for (final Parameter p : zrMethod.getMethodParameters()) {

			final Class<?> pType = p.getType();

			// 这里的顺序应该按实际使用频率从高到低排
			if (ZRequest.class == pType) {
				parameters[pI] = request;
				pI++;
				continue;
			}

			if (pType == ZRESPONSE_CLASS) {
				final ZResponse response = new ZResponse();
				parameters[pI] = response;
				pI++;
				continue;
			}

			if (pType == ZModel.class) {
				final ZModel model = new ZModel();
				parameters[pI] = model;
				pI++;
				continue;
			}

			final ZRequestParam requestParam = RU.getAnnotation(p, ZRequestParam.class);
			if (requestParam != null) {
				pI = Task.hZRequestParam(parameters, request, path, pI, p, requestParam);
				continue;
			}

			if (RU.isAnnotationPresent(p, ZPathVariable.class)) {
				final List<Object> list = ZPVTL.get();
				final Class<?> type = pType;
				// FIXME 2023年11月8日 下午4:39:18 zhanghen: @ZRM 启动校验是否此类型
				final Object v = list.get(zpvPI);
				try {
					Task.setZPathVariableValue(parameters, pI, type, v);
					zpvPI++;
				} catch (final NumberFormatException e) {
					throw new PathVariableException(p.getName() + STU.EQUALS + v, HttpStatusEnum.HTTP_400.getStatus());
				}

				// FIXME 2023年11月8日 下午10:47:54 zhanghen: TODO 继续支持 校验注解
				final ZMax zmax = RU.getAnnotation(p, ZMax.class);
				if (zmax != null) {
					ZValidator.validatedZMax(p, parameters[pI], zmax.max());
				}

				if (RU.isAnnotationPresent(p, ZPositive.class)) {
					ZValidator.validatedZPositive(p, parameters[pI]);
				}

				final ZMin zmin = RU.getAnnotation(p, ZMin.class);
				if (zmin != null) {
					ZValidator.validatedZMin(p, parameters[pI], zmin.min());
				}

				pI++;
				continue;
			}

			if (RU.isAnnotationPresent(p, ZRequestBody.class)) {
				final byte[] body = request.getBody();
				if (AU.isEmpty(body)) {
					final String simpleName = pType.getSimpleName();
					throw new FormPairParseException(
							"@" + ZRequestBody.class.getSimpleName() + " 参数 " + simpleName + " 不存在");
				}

				final Object object = J.parseObject(new String(body), pType);
				if (object == null) {
					final String simpleName = pType.getSimpleName();
					throw new FormPairParseException(
							"@" + ZRequestBody.class.getSimpleName() + " 参数 " + simpleName + " 错误");
				}

				Task.checkZValidated(p, object);

				parameters[pI] = object;
				pI++;
				continue;
			}

			final ZRequestHeader requestHeader = p.getAnnotation(ZRequestHeader.class);
			if (requestHeader != null) {
				final String name = requestHeader.value();
				final String headerValue = request.getHeader(name);
				if ((headerValue == null) && requestHeader.required()) {
					final String message = "请求方法[" + path + "]的header[" + p.getName() + "]不存在";
					throw new FormPairParseException(message, HttpStatusEnum.HTTP_400.getStatus());
				}
				parameters[pI] = headerValue;
				pI++;
				continue;
			}


			final ZCookieValue zcv = RU.getAnnotation(p, ZCookieValue.class);
			if (zcv != null) {
				final String cookieName = zcv.name();
				final ZCookie ck = request.getCookie(cookieName);
				if ((ck == null) && zcv.required()) {
					final String message = "请求方法[" + path + "]缺少名为[" + cookieName + "]的Cookie";
					throw new FormPairParseException(message, HttpStatusEnum.HTTP_400.getStatus());
				}

				if (ck != null) {
					if (pType == String.class) {
						parameters[pI] = ck.getValue();
						pI++;
					} else if (pType == ZCookie.class) {
						parameters[pI] = ck;
						pI++;
					}
				}

				continue;
			}

			if (pType == ZMultipartFile.class) {

				// FIXME 2024年12月23日 上午2:09:28 zhangzhen : 下面代码有一个可以正常运行的bug
				// 就是一个form-data如果只有一个文件而无其他内容，到此 request.getOriginalRequestBytes()
				// 内容其实header 下面一个空行 再下面是 --boundary--
				// 以后再看要不要修复，反正这个bug可以正常运行

				if (AU.isEmpty(request.getOriginalRequestBytes())) {
					throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在", HttpStatusEnum.HTTP_400.getStatus());
				}

				final List<FormData> fdList = HttpRequestParser.readFileFormData(request.getOriginalRequestBytes(),
						request.getContentType(), request.getBoundary());
				final Optional<FormData> findAny = fdList.stream().filter(fd -> fd.getName().equals(p.getName()))
						.findAny();
				if (!findAny.isPresent()) {
					throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在", HttpStatusEnum.HTTP_400.getStatus());
				}

				if (request.getTf() != null) {
					pI = tf(parameters, request, path, zpvPI, p);
				} else {
					// 走到这，说明是读到内存的，所以构造ByteArrayInputStream
					if (findAny.get().getBody() == null) {
						throw new FormPairParseException("上传文件的[" + p.getName() + "]的内容不存在",
								HttpStatusEnum.HTTP_400.getStatus());
					}

					final InputStream inputStream = new ByteArrayInputStream(findAny.get().getBody());
					final ZMultipartFile file = new ZMultipartFile(findAny.get().getName(),
							null,
							findAny.get().getFileName(),
							findAny.get().getBody(), false,
							findAny.get().getContentType(), inputStream, findAny.get().getBody().length);

					pI = Task.setValue(parameters, pI, p, file);
				}

				continue;
			}

		}

		return parameters;
	}

	private static int tf(final Object[] parameters, final ZRequest request,
			final String path, final int pI,
			final Parameter p) {
		if ((request.getTf() == null) || !p.getName().equals(request.getTf().getName())) {
			throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在", HttpStatusEnum.HTTP_400.getStatus());
		}

		final File file = request.getTf().getFile();
		// 走到这，说明是读到临时文件的的，所以从临时文件读
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}

		final String contentType = request.getTf().getContentType();

		final ZMultipartFile zmFile = new ZMultipartFile(request.getTf().getName(),
				request.getTf().getTempFilePath(),
				request.getTf().getFileName(),
				null, true,
				contentType, inputStream, file.length());

		final int nI = pI;
		final int newPI = Task.setValue(parameters, nI, p, zmFile);
		return newPI;
	}

	/**
	 * @ZPathVariable 支持的类型
	 */
	public final static Set<String> ZPV_TYPE = new HashSet<>();

	static {
		Collections.addAll(ZPV_TYPE, Byte.class.getName(), Short.class.getName(), Integer.class.getName(),
				Long.class.getName(), Float.class.getName(), Double.class.getName(), Boolean.class.getName(),
				Character.class.getName(), String.class.getName());
	}

	private static void setZPathVariableValue(final Object[] parameters, final int pI, final Class<?> type, final Object value) {
		if (type == Byte.class) {
			parameters[pI] = Byte.valueOf(String.valueOf(value));
		} else if (type == Short.class) {
			parameters[pI] = Short.valueOf(String.valueOf(value));
		} else if (type == Integer.class) {
			parameters[pI] = Integer.valueOf(String.valueOf(value));
		} else if (type == Long.class) {
			parameters[pI] = Long.valueOf(String.valueOf(value));
		} else if (type == Float.class) {
			parameters[pI] = Float.valueOf(String.valueOf(value));
		} else if (type == Double.class) {
			parameters[pI] = Double.valueOf(String.valueOf(value));
		} else if (type == Boolean.class) {
			parameters[pI] = Boolean.valueOf(String.valueOf(value));
		} else if (type == Character.class) {
			parameters[pI] = Character.valueOf(String.valueOf(value).charAt(0));
		} else if (type == String.class) {
			parameters[pI] = value;
		}

	}

	private static int hZRequestParam(final Object[] parameters, final ZRequest request, final String path,
			final int pI, final Parameter p, final ZRequestParam requestParam) {

		final List<RequestParam> params = request.getParams();
		if (CU.isNotEmpty(params)) {

			final RequestParam rp = findRequestParam(p, params);

			final Object value =
					(rp == null) ? null :
					(ZRequestParam.DEFAULT_NONE.equals(rp.getValue())) ? null
							: rp.getValue();

			if (requestParam.required() && (value == null)) {
				throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在",
						HttpStatusEnum.HTTP_400.getStatus());
			}

			try {
				return Task.setValue(parameters, pI, p, value);
			} catch (final NumberFormatException e) {
				throw new ParsingRequestParamException(p.getName() + STU.EQUALS + value,
						HttpStatusEnum.HTTP_400.getStatus());
			}

		}

		final byte[] body = request.getBody();
		if (AU.isEmpty(body)) {
			final String defaultValue = requestParam.defaultValue();
			if (defaultValue != null) {
				try {
					return Task.setValue(parameters, pI, p, defaultValue);
				} catch (final Exception e) {
					throw new FormPairParseException(p.getName() + " = " + defaultValue,
							HttpStatusEnum.HTTP_400.getStatus());
				}
			}
			throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在",
					HttpStatusEnum.HTTP_400.getStatus());
		}

		final List<FormData> fdList = HttpRequestParser.readFileFormData(request.getOriginalRequestBytes(),
				request.getContentType(), request.getBoundary());
		if (CU.isEmpty(fdList)) {
			throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在",
					HttpStatusEnum.HTTP_400.getStatus());
		}

		final Optional<FormData> findAny = fdList.stream()
				// FIXME 2024年12月21日 下午10:05:21 zhangzhen : 这个是isEmpty？是当时手误写错了？记得debug看下
				.filter(f -> STU.isEmpty(f.getFileName()))
				.filter(f -> f.getName().equals(p.getName()))
				.findAny();
		if (!findAny.isPresent()) {

			throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在",
					HttpStatusEnum.HTTP_400.getStatus());
		}

		return Task.setValue(parameters, pI, p, findAny.get().getValue());
	}

	private static RequestParam findRequestParam(final Parameter p, final List<RequestParam> paramlist) {
		for (final RequestParam requestParam : paramlist) {
			if(requestParam.getName().equals(p.getName())) {
				return requestParam;
			}
		}

		return null;
	}

	private static void checkZValidated(final Parameter p, final Object object) throws Exception {
		if (!p.isAnnotationPresent(ZValidated.class)) {
			return;
		}
		final ArrayList<Class<?>> pl = new ArrayList<>();
		pl.add(object.getClass());
		while (true) {
			final Class<?> superclass = pl.get(pl.size() - 1).getSuperclass();
			if (superclass == Object.class) {
				break;
			}
			pl.add(superclass);
		}

		Collections.reverse(pl);

		for (final Class<?> cls : pl) {
			final Field[] fs = cls.getDeclaredFields();
			for (final Field f1 : fs) {
				try {
					ZValidator.validatedAll(object, f1);
				} catch (final Exception e) {
					throw e;
				}
				checkT(object, f1);
			}
		}

	}

	/**
	 * 校验对象的某个字段如果是List/Set类型，则继续校验里面的泛型T是否也带有[校验注解]有则递归校验
	 *
	 * @param object
	 * @param field
	 */
	private static void checkT(final Object object, final Field field) {
		final Class<?> ftype = field.getType();
		// FIXME 2024年6月28日 下午5:44:43 zhangzhen : 忘了是否支持Map类型了，看@ZNotEmtpy的javadoc是支持Map的，记不清了是否支持了？
		if ((ftype == List.class) || (ftype == Set.class)) {
			final Iterable<?> it = (Iterable<?>) RU.getFiledValue(object, field);
			if (it != null) {
				for (final Object lv : it) {
					final Field[] lvfs = lv.getClass().getDeclaredFields();
					for (final Field lf : lvfs) {
						try {
							ZValidator.validatedAll(lv, lf);
						} catch (final Exception e) {
							e.printStackTrace();
						}
						checkT(lv, lf);
					}
				}
			}
		}
	}

	private static int setValue(final Object[] parameters, final int pI, final Parameter parameter, final Object value)
			throws NumberFormatException {

		final Class<?> parameterType = parameter.getType();
		final AtomicInteger nI = new AtomicInteger(pI);
		if (parameterType == Byte.class) {
			parameters[nI.getAndIncrement()] =
					value == null ? null :
					Byte.valueOf(
					value instanceof String ? (String)value : String.valueOf(value));
		} else if (parameterType == Short.class) {
			parameters[nI.getAndIncrement()] =
					value == null ? null :
					Short.valueOf(
					value instanceof String ? (String)value : String.valueOf(value));
		} else if (parameterType == Integer.class) {
			parameters[nI.getAndIncrement()] =
					value == null ? null :
					Integer.valueOf(
					value instanceof String ? (String)value : String.valueOf(value));
		} else if (parameterType == Long.class) {
			parameters[nI.getAndIncrement()] =
					value == null ? null :
					Long.valueOf(
					value instanceof String ? (String)value : String.valueOf(value));
		} else if (parameterType == Float.class) {
			parameters[nI.getAndIncrement()] =
					value == null ? null :
					Float.valueOf(
					value instanceof String ? (String)value : String.valueOf(value));
		} else if (parameterType == Double.class) {
			parameters[nI.getAndIncrement()] =
					value == null ? null :
					Double.valueOf(
					value instanceof String ? (String)value : String.valueOf(value));
		} else if (parameterType == Character.class) {
			parameters[nI.getAndIncrement()] =
					value == null ? null :
					Character.valueOf((
					value instanceof String ? (String)value : String.valueOf(value)).charAt(0));
		} else if (parameterType == Boolean.class) {
			parameters[nI.getAndIncrement()] =
					value == null ? null :
					Boolean.valueOf(
					value instanceof String ? (String)value : String.valueOf(value));
		} else {
			parameters[nI.getAndIncrement()] = value;
		}

		return nI.get();
	}

	private static Object[] generateParameters(final ZRequest request, final String path, final ZRMethod zrMethod)
			throws Exception {

		if (zrMethod.getMethodParameterSize() <= 0) {
			return EMPTY_OBJECT_ARRAY;
		}

		return Task.generateParameters0(request, path, zrMethod);
	}

	private static void setZRequestAndZResponse(final ZRequest request, final Object[] parameterArray) {

		if (AU.isEmpty(parameterArray)) {
			return;
		}

		boolean sR = false;
		for (final Object param : parameterArray) {
			if (param == null) {
				continue;
			}

			if (ZRESPONSE_CLASS == param.getClass()) {
				ZHttpContext.setZResponse((ZResponse) param);
				sR = true;
				break;
			}
		}

		if (!sR) {
			ZHttpContext.setZResponse(new ZResponse());
		}
	}


}
