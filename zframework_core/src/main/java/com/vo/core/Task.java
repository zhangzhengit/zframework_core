package com.vo.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.channels.SocketChannel;
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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.vo.anno.ZCookieValue;
import com.vo.anno.ZRequestBody;
import com.vo.anno.ZRequestHeader;
import com.vo.aop.InterceptorParameter;
import com.vo.cache.AU;
import com.vo.cache.CU;
import com.vo.cache.J;
import com.vo.cache.STU;
import com.vo.common.CR;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.core.ZRequest.RequestParam;
import com.vo.enums.MethodEnum;
import com.vo.exception.FormPairParseException;
import com.vo.exception.PathVariableException;
import com.vo.exception.ResourceNotExistException;
import com.vo.html.ResourcesLoader;
import com.vo.http.AccessDeniedCodeEnum;
import com.vo.http.CTEnum;
import com.vo.http.HttpStatusEnum;
import com.vo.http.ZControllerMap;
import com.vo.http.ZCookie;
import com.vo.http.ZPVTL;
import com.vo.http.ZQPSLimitation;
import com.vo.http.ZRMethod;
import com.vo.http.ZRequestParam;
import com.vo.scanner.ZHandlerInterceptor;
import com.vo.scanner.ZHandlerInterceptorScanner;
import com.vo.scanner.ZModelAndView;
import com.vo.template.ZModel;
import com.vo.template.ZTemplate;
import com.vo.validator.ParsingRequestParamException;
import com.vo.validator.ZFException;
import com.vo.validator.ZMin;
import com.vo.validator.ZPositive;
import com.vo.validator.ZValidated;
import com.vo.validator.ZValidator;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年7月3日
 *
 */
public class Task {

	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES = ZContext
			.getBean(ServerConfigurationProperties.class);
	private static final RequestValidatorConfigurationProperties REQUEST_VALIDATOR_CONFIGURATION_PROPERTIES = ZContext.getBean(RequestValidatorConfigurationProperties.class);

	public static final String SP = "&";
	public static final String DEFAULT_CHARSET_NAME = Charset.defaultCharset().displayName();
	public static final String VOID = "void";
	public static final String HTTP_200 = "HTTP/1.1 200";
	public static final int HTTP_STATUS_500 = 500;
	public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
	public static final ContentTypeEnum DEFAULT_CONTENT_TYPE = ContentTypeEnum.APPLICATION_JSON;
	public static final ThreadLocal<SocketChannel> SCTL = new ThreadLocal<>();
	private final SocketChannel socketChannel;

	public Task(final SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}

	/**
	 * 根据请求头信息获取目标接口方法的特定注解
	 *
	 * @param request
	 * @param annoClass TODO
	 * @return
	 * @throws Exception
	 */
	static <T extends Annotation> T getMethodAnnotation(final ZRequest request, final Class<T> annoClass) {

		final String key = request.getRequestURI() + '@' + annoClass.getName()  + '-' + annoClass.hashCode();

		return ZRC.singleton().computeIfAbsent(key, () -> getMethodAnnotation0(request, annoClass));
	}

	public static <T extends Annotation> T getMethodAnnotation0(final ZRequest request, final Class<T> annoClass) {
		// 匹配path
		if (CU.isEmpty(request.getLineList())) {
			return null;
		}

		final String path = request.getPath();
		final ZRMethod zrMethod = ZControllerMap.getMethodByMethodEnumAndPath(request.getMethodEnum(), path);
		if (zrMethod == null) {

			final Map<String, ZRMethod> rowMap = ZControllerMap.getByMethodEnum(request.getMethodEnum());
			final Set<Entry<String, ZRMethod>> entrySet = rowMap.entrySet();
			for (final Entry<String, ZRMethod> entry : entrySet) {
				final ZRMethod methodTarget = entry.getValue();
				final String requestMapping = entry.getKey();
				if (Boolean.TRUE.equals(ZControllerMap.getIsregexByMethodEnumAndPath(methodTarget.getMethod(), requestMapping))
						&& path.matches(requestMapping)) {
					return methodTarget.getMethod().getAnnotation(annoClass);
				}
			}

			return null;
		}

		return zrMethod.getMethod().getAnnotation(annoClass);
	}

	/**
	 * 执行目标方法（接口Method）
	 *
	 * @param request 请求体
	 * @param socketChannel TODO
	 * @return 响应结果，已根据具体的方法处理好header、cookie、body等内容，只是没write
	 * @throws Exception
	 *
	 */
	public ZResponse invoke(final ZRequest request, final SocketChannel socketChannel) throws Exception {

		final String path = request.getPath();
		ZRMethod zrMethod = ZControllerMap.getMethodByMethodEnumAndPath(request.getMethodEnum(), path);

		// 查找对应的控制器来处理
		if (zrMethod == null) {

			// 用非请求的METHOD看是否有，有则响应405
			final ZRMethod noRequestMethodMethod = ZRC.singleton().computeIfAbsent("MethodEnum.values-" + path, () -> {
				final MethodEnum[] es = MethodEnum.values();
				for (final MethodEnum methodEnum : es) {
					if (methodEnum != request.getMethodEnum()) {
						final ZRMethod methodT = ZControllerMap.getMethodByMethodEnumAndPath(methodEnum, path);
						if (methodT != null) {
							return methodT;
						}
					}
				}
				return null;
			}, true);

			if (noRequestMethodMethod != null) {
				return ReU.response405(socketChannel, request.getMethodEnum().getMethod());
			}

			// 用正则依然匹配不到，响应404
			final ZRMethod matcheZRMethod = Task.getMatcheMethod(request, path);
			if (matcheZRMethod == null) {
				return ReU.response404(socketChannel, path);
			}

			zrMethod = matcheZRMethod;
		}

		try {
			if (zrMethod.isVoid()) {
				ZRSC.set(socketChannel);
			}

			// 找到目标方法了，开始生成参数了
			final Object[] parameterArray = this.generateParameters(zrMethod.getMethod(), request, path);
			if (parameterArray == null) {
				return null;
			}

			final Object zController = ZControllerMap.getObjectByMethod(zrMethod.getMethod());
			final ZResponse re = this.invokeAndResponse(zrMethod, parameterArray, zController, request);
			return re;

		} catch (final Exception e) {
			//			e.printStackTrace();
			throw e;
		}

	}

	private static ZRMethod getMatcheMethod(final ZRequest request,  final String path) throws Exception {

		final Supplier<ZRMethod> supplier = () -> {
			final Map<String, ZRMethod> rowMap = ZControllerMap.getByMethodEnum(request.getMethodEnum());
			final Set<Entry<String, ZRMethod>> entrySet = rowMap.entrySet();
			for (final Entry<String, ZRMethod> entry : entrySet) {
				final ZRMethod methodTarget = entry.getValue();
				final String requestMapping = entry.getKey();
				if (Boolean.TRUE.equals(ZControllerMap.getIsregexByMethodEnumAndPath(methodTarget.getMethod(), requestMapping))
						&& path.matches(requestMapping)) {

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

		final String zfm = getZFMessage(e);
		final String eMessage =  (STU.isEmpty(zfm) ? "" : "\r\n\tmessage=" + zfm + "\r\n\t")
				+stringWriter
				;

		return eMessage;
	}

	private static String getZFMessage(final Throwable e) {
		if (e instanceof ZFException) {
			final ZFException ev = (ZFException) e;
			return ev.getMessagezf();
		}
		
		final Throwable cause = e.getCause();
		if (cause instanceof ZFException) {
			final ZFException ev1 = (ZFException) cause;
			return ev1.getMessagezf();
		}
		
		return e.getLocalizedMessage();
	}

	private void close() {
		// socketChannel 不关闭
		//		if (this.socketChannel != null) {
		//		}
	}

	@SuppressWarnings("boxing")
	private ZResponse invokeAndResponse(final ZRMethod zrMethod, final Object[] parametersArray, final Object zControllerObject, final ZRequest request)
			throws IllegalAccessException, InvocationTargetException, IOException {
		
		final String controllerName = zControllerObject.getClass().getName();
		final Integer qps = ZControllerMap.getQPSByControllerNameAndMethodName(controllerName, zrMethod.getMethod().getName());

		final QCTimeEnum qcTimeEnum = ZControllerMap.getQCTimeByControllerNameAndMethodName(controllerName, zrMethod.getMethod().getName());
		
		final QPSHandlingEnum handlingEnum = REQUEST_VALIDATOR_CONFIGURATION_PROPERTIES.getHandlingEnum(request.getUserAgent());
		final boolean allow = QC.allow(qcTimeEnum,
				"a-" + controllerName.hashCode() + '@' + zrMethod.getMethod().getName().hashCode(), qps,
				handlingEnum);
		if (!allow) {

			// FIXME 2025年1月3日 上午4:19:33 zhangzhen : 这里有个严重的问题会导致可能浪费服务器性能和存储空间
			// 尤其是上传文件尤其是很大的文件时，因为当前逻辑是解析完body并且save到临时文件之后，才会走到
			// 什么的判断api.qps的部分，所以频繁上传可能再次导致不执行api
			// 前几天写的功能[自定义http解析流程]，似乎可以把这个部分逻辑放进去，
			// 即：先解析header如果API.qps超了，则不解析body


			final CR<Object> error = CR.error(AccessDeniedCodeEnum.API.getCode(),
					AccessDeniedCodeEnum.API.getInternalMessage());

			final ZResponse response = new ZResponse(this.socketChannel);
			response.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
			.httpStatus(HttpStatusEnum.HTTP_429.getCode())
			.body(J.toJSONString(error, Include.NON_NULL));

			return response;
		}

		// 是否超过 ZQPSLimitation.qps
		final ZQPSLimitation zqpsLimitation = ZControllerMap.getZQPSLimitationByControllerNameAndMethodName(controllerName,
				zrMethod.getMethod().getName());
		if (zqpsLimitation != null) {

			switch (zqpsLimitation.type()) {

			case ZSESSIONID:
				if (!SERVER_CONFIGURATIONPROPERTIES.isResponseZSessionId()) {
					break;
				}
					
				final ZSession session = Task.getOrGSession(request);
				final String keyword = controllerName
						+ "@" + zrMethod.getMethod().getName()
						+ "@ZQPSLimitation" + '_'
						+ session.getId();

				if (!QC.allow(zqpsLimitation.time(), keyword, zqpsLimitation.count(), handlingEnum)) {
					//				if (!QC.allow(QCTimeEnum.SECOND, keyword, zqpsLimitation.count(), handlingEnum)) {

					final CR<Object> error = CR.error(AccessDeniedCodeEnum.ZSESSIONID.getCode(), AccessDeniedCodeEnum.ZSESSIONID.getMessageToClient());
					final ZResponse response = new ZResponse(this.socketChannel);
					response.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
					.httpStatus(HttpStatusEnum.HTTP_429.getCode())
					.body(J.toJSONString(error, Include.NON_NULL));

					if (SERVER_CONFIGURATIONPROPERTIES.isResponseZSessionId()) {
						NioLongConnectionServer.setZSessionId(request, response);
					}

					return response;
				}
				break;

			default:
				break;
			}
		}

		ZResponseStatus.initialization();
		
		this.setZRequestAndZResponse(parametersArray, request, zrMethod);

		Object r = null;
		// 在此zhi执行
		final List<ZHandlerInterceptor> zhiList = ZHandlerInterceptorScanner.match(request.getRequestURI());
		if (CU.isEmpty(zhiList)) {
			r = invoke0(zrMethod.getMethod(), parametersArray, zControllerObject);
		} else {
			final ZResponse response = new ZResponse(this.socketChannel);
			final ArrayList<Object> pa = new ArrayList<>();
			Collections.addAll(pa, parametersArray);
			final InterceptorParameter interceptorParameter = new InterceptorParameter(zrMethod.getMethod().getName(), zrMethod.getMethod(),
					zrMethod.getMethod().getReturnType().getName().equals(Void.class.getName()),
					pa, zControllerObject);
			// 1 按从小到大执行pre
			boolean stop = false;
			for (final ZHandlerInterceptor zhi : zhiList) {
				final boolean preHandle = zhi.preHandle(request, response, interceptorParameter);
				if (!preHandle) {
					stop = true;
					break;
				}
			}

			// 有 preHandle 返回false，直接返回response（在preHandle可能设值了）
			if (stop) {
				return response;
			}

			if (!stop) {

				r = invoke0(zrMethod.getMethod(), parametersArray, zControllerObject);
				final ZModelAndView modelAndView =
						zrMethod.getCtEnum() == CTEnum.NORMAL
						? new ZModelAndView(true, String.valueOf(r), readHtmlContent(r), ZModel.get(),
								(ZModel) Arrays.stream(parametersArray).filter(arg -> arg.getClass().equals(ZModel.class))
								.findAny().orElse(null),
								null)
								: new ZModelAndView(false, null, null, null, (ZModel) null, r);

				// 2 按从大到小执行post
				for (int i = zhiList.size() - 1; i >= 0; i--) {
					final ZHandlerInterceptor zhi = zhiList.get(i);
					zhi.postHandle(request, response, interceptorParameter, modelAndView);
				}
				// 3 按从大到小执行after
				for (int i = zhiList.size() - 1; i >= 0; i--) {
					final ZHandlerInterceptor zhi = zhiList.get(i);
					zhi.afterCompletion(request, response, interceptorParameter, modelAndView);
				}
			}
		}

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
				return new ZResponse(this.socketChannel)
						.contentType(ContentTypeEnum.APPLICATION_JSON.getType());
			}
			
			// 有ZR参数未CT，根据produces然后看类的注解
			final String contentType = response.getContentType();
			if (contentType == null) {
				final String p1 = findProduces(request, zrMethod.getProduces());
				response.contentType(p1==null ? DEFAULT_CONTENT_TYPE.getType() : p1);
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
			if ((ps.length == 1)) {
				return this.responseCT(r, ps[0], zrMethod.getCtea()[0]);
			}
			final int x = 20;
			// FIXME 2025年12月6日 00:39:34 zhangzhen : 多个ps的待会再做，先做下面简单的

		}
		
		// 第三优先：@ZResponseBody 注解，返回类型String则响应text/plain
		// 否则一律application/json
		if (zrMethod.hasResponseBody()) {
			if (zrMethod.isRTString()) {
				return this.responseTextPlain(r);
			}
			return this.responseAppJSON(r);
		}
		
		// 第4优先：@ZRestCon还是@ZCon注解,ZC则默认为html名称，
		// ZRC则区分returnType为String则CT为text/plain，其他一律json
		final CTEnum ctEnum = zrMethod.getCtEnum();
		// 响应 html
		if ((ctEnum == CTEnum.NORMAL) ) {
			return this.responseHtml(r);
		}
		
		if ((ctEnum == CTEnum.REST) && zrMethod.isRTString()) {
			return this.responseTextPlain(r);
		}
		
		// 默认响应json
		return this.responseAppJSON(r);
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
		final ZSession sessionFAlSE = request.getSession(false);
		if (sessionFAlSE != null) {
			return sessionFAlSE;
		}

		return request.getSession(true);
	}

	/**
	 * 真正的API目标方法执行，统一在本方法里面执行，方便统一处理
	 *
	 * @param apiMethod		要执行的API的method
	 * @param pArray		此method的参数数组，如：ZRequest/ZModel/@ZRequestHeader/@ZRequestParam等等
	 * @param zControllerObject	此method所在的 @ZController 标记的对象
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	private static Object invoke0(final Method apiMethod, final Object[] pArray, final Object zControllerObject)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {


		final Object r = apiMethod.invoke(zControllerObject, pArray);

		if (pArray.length > 0) {
			try {
				closeZMFInputStreamAndDeleteTempFile(pArray);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		return r;
	}

	private static void closeZMFInputStreamAndDeleteTempFile(final Object[] arraygP) throws IOException {

		for (int i = arraygP.length - 1; i >= 0; i--) {
			if (ZMultipartFile.class.equals(arraygP[i].getClass())) {
				final ZMultipartFile file = (ZMultipartFile) arraygP[i];
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

	private ZResponse responseCT(final Object r, final String contentType, final ContentTypeEnum cte) {
		final ZResponse rx = new ZResponse(this.socketChannel).contentType(contentType);
		cte.body(r, rx);
		return rx;
	}
	
	private ZResponse responseTextPlain(final Object r) {
		return new ZResponse(this.socketChannel).contentType(ContentTypeEnum.TEXT_PLAIN.getType()).body(r instanceof String ? (String) r : String.valueOf(r));
	}

	private ZResponse responseAppJSON(final Object r) {
		final String json = J.toJSONString(r, Include.NON_NULL);
		return new ZResponse(this.socketChannel).contentType(DEFAULT_CONTENT_TYPE.getType()).body(json);
	}

	private ZResponse responseHtml(final Object r) {
		try {

			final String htmlContent = readHtmlContent(r);

			final String html = ZTemplate.freemarker(r instanceof String ? (String)r : String.valueOf(r), htmlContent);
			ZModel.clear();

			return new ZResponse(this.socketChannel).contentType(ContentTypeEnum.TEXT_HTML.getType()).body(html);

		} catch (final Exception e) {
			e.printStackTrace();
			final String em = Task.gExceptionMessage(e);
			
			if (e instanceof ResourceNotExistException) {
				final ResourceNotExistException ex = (ResourceNotExistException) e;
				return new ZResponse(this.socketChannel)
						.httpStatus(ex.getHttpStatus())
						.contentType(DEFAULT_CONTENT_TYPE.getType())
						.body(J.toJSONString(CR.error(ex.getMessagezf()),Include.NON_NULL));
			}
			
			return new ZResponse(this.socketChannel)
					.httpStatus(HttpStatusEnum.HTTP_500.getCode())
					.contentType(DEFAULT_CONTENT_TYPE.getType())
					.body(J.toJSONString(CR.error(em),Include.NON_NULL));
		}
	}

	private static String readHtmlContent(final Object r) {
		final String ss = String.valueOf(r);
		final String htmlName = ss.charAt(0) == '/' ? ss : '/' + ss;
		final String htmlContent = ResourcesLoader.loadStaticResourceString(htmlName);
		return htmlContent;
	}

	private Object[] generateParameters(final Method method, final Object[] parametersArray, final ZRequest request,
			final String path) throws NumberFormatException {

		final Parameter[] ps = RU.getParameters(method);
		if (ps.length < parametersArray.length) {
			throw new IllegalArgumentException("方法参数个数小于数组length,method = " + method.getName()
			+ " parametersArray.length = " + parametersArray.length);
		}

		int pI = 0;
		int zpvPI = 0;
		for (final Parameter p : ps) {
			if (p.isAnnotationPresent(ZRequestHeader.class)) {
				final ZRequestHeader a = RU.getAnnotation(p, ZRequestHeader.class);
				final String name = a.value();
				final String headerValue = request.getHeaderMap().get(name);
				if ((headerValue == null) && a.required()) {
					final String message = "请求方法[" + path + "]的header[" + p.getName() + "]不存在";
					throw new FormPairParseException(message, HttpStatusEnum.HTTP_400.getCode());
				}
				parametersArray[pI] = headerValue;
				pI++;
			} else {
				final Class<?> pType = p.getType();
				if (RU.isAnnotationPresent(p, ZCookieValue.class)) {
					final ZCookieValue cookieValue = RU.getAnnotation(p, ZCookieValue.class);
					final String cookieName = STU.isEmpty(cookieValue.name()) ? p.getName() : cookieValue.name();
					final ZCookie[] cookies = request.getCookies();
					final String message = "请求方法[" + path + "]缺少名为[" + cookieName + "]的Cookie";
					if (AU.isEmpty(cookies)) {
						if (cookieValue.required()) {
							throw new FormPairParseException(message, HttpStatusEnum.HTTP_400.getCode());
						}
					} else {

						final Optional<ZCookie> c = Arrays.stream(cookies)
								.filter(cookie -> Objects.equals(cookie.getName(), cookieName)).findAny();
						if (c.isPresent()) {
							if (pType == (String.class)) {
								parametersArray[pI] = c.get().getValue();
								pI++;
							} else if (pType == ZCookie.class) {
								parametersArray[pI] = c.get();
								pI++;
							}
						} else {
							if (cookieValue.required()) {
								throw new FormPairParseException(message, HttpStatusEnum.HTTP_400.getCode());
							}
							parametersArray[pI] = null;
							pI++;
						}
					}

				} else if (ZRequest.class == pType) {
					parametersArray[pI] = request;
					pI++;
				} else if (pType == ZResponse.class) {
					final ZResponse response = new ZResponse(this.socketChannel);
					parametersArray[pI] = response;
					pI++;
				} else if (pType == ZModel.class) {
					final ZModel model = new ZModel();
					parametersArray[pI] = model;
					pI++;
				} else if (RU.isAnnotationPresent(p, ZRequestBody.class)) {
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

					parametersArray[pI] = object;
					pI++;

				} else if (RU.isAnnotationPresent(p, ZRequestParam.class)) {
					pI = Task.hZRequestParam(parametersArray, request, path, pI, p);
				} else if (RU.isAnnotationPresent(p, ZPathVariable.class)) {
					final List<Object> list = ZPVTL.get();
					final Class<?> type = pType;
					// FIXME 2023年11月8日 下午4:39:18 zhanghen: @ZRM 启动校验是否此类型
					final Object v = list.get(zpvPI);
					try {
						Task.setZPathVariableValue(parametersArray, pI, type, v);
						zpvPI++;
					} catch (final NumberFormatException e) {
						throw new PathVariableException(p.getName() + STU.EQUALS + v, HttpStatusEnum.HTTP_400.getCode());
					}

					// FIXME 2023年11月8日 下午10:47:54 zhanghen: TODO 继续支持 校验注解
					if (RU.isAnnotationPresent(p, ZPositive.class)) {
						ZValidator.validatedZPositive(p, parametersArray[pI]);
					}
					if (RU.isAnnotationPresent(p, ZMin.class)) {
						ZValidator.validatedZMin(p, parametersArray[pI], RU.getAnnotation(p, ZMin.class).min());
					}
					pI++;
				} else if (pType == ZMultipartFile.class) {

					// FIXME 2024年12月23日 上午2:09:28 zhangzhen : 下面代码有一个可以正常运行的bug
					// 就是一个form-data如果只有一个文件而无其他内容，到此 request.getOriginalRequestBytes()
					// 内容其实header 下面一个空行 再下面是 --boundary--
					// 以后再看要不要修复，反正这个bug可以正常运行

					if (AU.isEmpty(request.getOriginalRequestBytes())) {
						throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在", HttpStatusEnum.HTTP_400.getCode());
					}

					final List<FD2> fdList = BodyReader.readFormData(request.getOriginalRequestBytes(),
							request.getContentType(), request.getBoundary());
					final Optional<FD2> findAny = fdList.stream().filter(fd -> fd.getName().equals(p.getName()))
							.findAny();
					if (findAny.isPresent()) {
						// 走到这，说明是读到内存的，所以构造ByteArrayInputStream

						if (findAny.get().getBody() == null) {
							throw new FormPairParseException("上传文件的[" + p.getName() + "]的内容不存在",
									HttpStatusEnum.HTTP_400.getCode());
						}
						
						final InputStream inputStream = new ByteArrayInputStream(findAny.get().getBody());
						final ZMultipartFile file = new ZMultipartFile(findAny.get().getName(),
								null,
								findAny.get().getFileName(),
								findAny.get().getBody(), false,
								findAny.get().getContentType(), inputStream);

						pI = Task.setValue(parametersArray, pI, p, file);

					} else {

						if ((request.getTf() == null) || !p.getName().equals(request.getTf().getName())) {
							throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在", HttpStatusEnum.HTTP_400.getCode());
						}

						// 走到这，说明是读到临时文件的的，所以从临时文件读
						InputStream inputStream = null;
						try {
							inputStream = new FileInputStream(request.getTf().getFile());
						} catch (final FileNotFoundException e) {
							e.printStackTrace();
						}

						final String contentType = request.getTf().getContentType();

						final ZMultipartFile file = new ZMultipartFile(request.getTf().getName(),
								request.getTf().getTempFilePath(),
								request.getTf().getFileName(),
								null, true,
								contentType, inputStream);

						pI = Task.setValue(parametersArray, pI, p, file);
					}

				}
			}

		}

		return parametersArray;
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

	private static void setZPathVariableValue(final Object[] parametersArray, final int pI, final Class<?> type, final Object value) {
		if (type.getName().equals(Byte.class.getName())) {
			parametersArray[pI] = Byte.valueOf(String.valueOf(value));
		} else if (type.getName().equals(Short.class.getName())) {
			parametersArray[pI] = Short.valueOf(String.valueOf(value));
		} else if (type.getName().equals(Integer.class.getName())) {
			parametersArray[pI] = Integer.valueOf(String.valueOf(value));
		} else if (type.getName().equals(Long.class.getName())) {
			parametersArray[pI] = Long.valueOf(String.valueOf(value));
		} else if (type.getName().equals(Float.class.getName())) {
			parametersArray[pI] = Float.valueOf(String.valueOf(value));
		} else if (type.getName().equals(Double.class.getName())) {
			parametersArray[pI] = Double.valueOf(String.valueOf(value));
		} else if (type.getName().equals(Boolean.class.getName())) {
			parametersArray[pI] = Boolean.valueOf(String.valueOf(value));
		} else if (type.getName().equals(Character.class.getName())) {
			parametersArray[pI] = Character.valueOf(String.valueOf(value).charAt(0));
		} else if (type.getName().equals(String.class.getName())) {
			parametersArray[pI] = String.valueOf(value);
		}
	}

	private static int hZRequestParam(final Object[] parametersArray, final ZRequest request, final String path,
			final int pI, final Parameter p) {

		int piR = 0;
		final Set<RequestParam> paramSet = request.getParamSet();
		if (CU.isNotEmpty(paramSet)) {
			final Optional<RequestParam> findAny = paramSet.stream()
					.filter(rp -> rp.getName().equals(p.getName()))
					.findAny();
			if (!findAny.isPresent()) {
				throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在",
						HttpStatusEnum.HTTP_400.getCode());
			}

			final Object value = findAny.get().getValue();
			if (value != null) {
				try {
					piR = Task.setValue(parametersArray, pI, p, findAny.get().getValue());
				} catch (final NumberFormatException e) {
					throw new ParsingRequestParamException(p.getName() + STU.EQUALS + findAny.get().getValue(),
							HttpStatusEnum.HTTP_400.getCode());
				}
			} else {
				final String defaultValue = p.getAnnotation(ZRequestParam.class).defaultValue();
				if (defaultValue != null) {
					try {
						piR = Task.setValue(parametersArray, pI, p, defaultValue);
					} catch (final Exception e) {
						e.printStackTrace();
						throw new FormPairParseException(p.getName() + " = " + defaultValue,
								HttpStatusEnum.HTTP_400.getCode());
					}
				}
			}

		} else {
			final byte[] body = request.getBody();
			if (AU.isEmpty(body)) {
				final String defaultValue = p.getAnnotation(ZRequestParam.class).defaultValue();
				if (defaultValue != null) {
					try {
						piR = Task.setValue(parametersArray, pI, p, defaultValue);
					} catch (final Exception e) {
						throw new FormPairParseException(p.getName() + " = " + defaultValue,
								HttpStatusEnum.HTTP_400.getCode());
					}
					return piR;
				}
				throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在",
						HttpStatusEnum.HTTP_400.getCode());
			}

			final List<FD2> fdList = BodyReader.readFormData(request.getOriginalRequestBytes(),
					request.getContentType(), request.getBoundary());
			if (CU.isEmpty(fdList)) {
				throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在",
						HttpStatusEnum.HTTP_400.getCode());
			}

			final Optional<FD2> findAny = fdList.stream()
					// FIXME 2024年12月21日 下午10:05:21 zhangzhen : 这个是isEmpty？是当时手误写错了？记得debug看下
					.filter(f -> STU.isEmpty(f.getFileName()))
					.filter(f -> f.getName().equals(p.getName()))
					.findAny();
			if (!findAny.isPresent()) {
				throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在",
						HttpStatusEnum.HTTP_400.getCode());
			}

			piR = Task.setValue(parametersArray, pI, p, findAny.get().getValue());
		}
		return piR;
	}

	private static void checkZValidated(final Parameter p, final Object object) {
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
				ZValidator.validatedAll(object, f1);
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
			try {
				field.setAccessible(true);
				final Iterable<?> it = (Iterable<?>) field.get(object);
				if (it != null) {
					for (final Object lv : it) {
						final Field[] lvfs = lv.getClass().getDeclaredFields();
						for (final Field lf : lvfs) {
							ZValidator.validatedAll(lv, lf);
							checkT(lv, lf);
						}
					}
				}

			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	private static int setValue(final Object[] parametersArray, final int pI, final Parameter parameter, final Object value)
			throws NumberFormatException {

		final Class<?> parameterType = parameter.getType();
		final AtomicInteger nI = new AtomicInteger(pI);
		if (parameterType == Byte.class) {
			parametersArray[nI.getAndIncrement()] = Byte.valueOf(
					value instanceof String ? (String)value : String.valueOf(value));
		} else if (parameterType == Short.class) {
			parametersArray[nI.getAndIncrement()] = Short.valueOf(
					value instanceof String ? (String)value : String.valueOf(value));
		} else if (parameterType == Integer.class) {
			parametersArray[nI.getAndIncrement()] = Integer.valueOf(
					value instanceof String ? (String)value : String.valueOf(value));
		} else if (parameterType == Long.class) {
			parametersArray[nI.getAndIncrement()] = Long.valueOf(
					value instanceof String ? (String)value : String.valueOf(value));
		} else if (parameterType == Float.class) {
			parametersArray[nI.getAndIncrement()] = Float.valueOf(
					value instanceof String ? (String)value : String.valueOf(value));
		} else if (parameterType == Double.class) {
			parametersArray[nI.getAndIncrement()] = Double.valueOf(
					value instanceof String ? (String)value : String.valueOf(value));
		} else if (parameterType == Character.class) {
			parametersArray[nI.getAndIncrement()] = Character.valueOf((
					value instanceof String ? (String)value : String.valueOf(value)).charAt(0));
		} else if (parameterType == Boolean.class) {
			parametersArray[nI.getAndIncrement()] = Boolean.valueOf(
					value instanceof String ? (String)value : String.valueOf(value));
		} else {
			parametersArray[nI.getAndIncrement()] = value;
		}

		return nI.get();
	}

	private Object[] generateParameters(final Method method, final ZRequest request, final String path)
			throws
			NumberFormatException
	{
		final Object[] parametersArray = new Object[method.getParameterCount()];
		return this.generateParameters(method, parametersArray, request, path);
	}

	private void setZRequestAndZResponse(final Object[] parameterArray, final ZRequest request, final ZRMethod zrmethod) {

		if (parameterArray == null) {
			return;
		}

		ZHttpContext.setZRequest(request);

		boolean sR = false;
		for (final Object object : parameterArray) {
			if (object == null) {
				continue;
			}

			if (ZResponse.class == object.getClass()) {
				ZHttpContext.setZResponse((ZResponse) object);
				sR = true;
				break;
			}
		}

		if (!sR) {
			ZHttpContext.setZResponse(new ZResponse(this.socketChannel));
		}
	}

}
