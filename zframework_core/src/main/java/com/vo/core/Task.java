package com.vo.core;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URLDecoder;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.vo.anno.ZCookieValue;
import com.vo.anno.ZRequestBody;
import com.vo.anno.ZRequestHeader;
import com.vo.aop.InterceptorParameter;
import com.vo.api.StaticController;
import com.vo.cache.AU;
import com.vo.cache.CU;
import com.vo.cache.J;
import com.vo.cache.STU;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.core.ZRequest.RequestParam;
import com.vo.enums.MethodEnum;
import com.vo.exception.FormPairParseException;
import com.vo.exception.PathVariableException;
import com.vo.exception.ZControllerAdviceThrowable;
import com.vo.html.ResourcesLoader;
import com.vo.http.AccessDeniedCodeEnum;
import com.vo.http.HttpStatus;
import com.vo.http.ZControllerMap;
import com.vo.http.ZCookie;
import com.vo.http.ZHtml;
import com.vo.http.ZPVTL;
import com.vo.http.ZQPSLimitation;
import com.vo.http.ZRequestParam;
import com.vo.scanner.ZHandlerInterceptor;
import com.vo.scanner.ZHandlerInterceptorScanner;
import com.vo.scanner.ZModelAndView;
import com.vo.template.ZModel;
import com.vo.template.ZTemplate;
import com.vo.validator.ZFException;
import com.vo.validator.ZMin;
import com.vo.validator.ZPositive;
import com.vo.validator.ZValidated;
import com.vo.validator.ZValidator;
import com.votool.common.CR;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年7月3日
 *
 */
public class Task {

	public static final String SP = "&";
	public static final String EMPTY_STRING = "";

	public static final String DEFAULT_CHARSET_NAME = Charset.defaultCharset().displayName();
	public static final String VOID = "void";
	public static final String HTTP_200 = "HTTP/1.1 200";
	public static final int HTTP_STATUS_500 = 500;
	public static final int HTTP_STATUS_404 = 404;
	public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
	public static final HeaderEnum DEFAULT_CONTENT_TYPE = HeaderEnum.APPLICATION_JSON;
	public static final String NEW_LINE = "\r\n";
	private static final Map<Object, Object> CACHE_MAP = new WeakHashMap<>(1024, 1F);

	public static final ThreadLocal<SocketChannel> SCTL = new ThreadLocal<>();
	private final SocketChannel socketChannel;
	private final Socket socket;
	private BufferedInputStream bufferedInputStream;
	private InputStream inputStream;
	private OutputStream outputStream;

	public Task(final SocketChannel socketChannel) {
		SCTL.set(socketChannel);
		this.socketChannel = socketChannel;
		this.socket = null;
	}

	public Task(final Socket socket) {
		this.socketChannel = null;
		this.socket = socket;
		try {
			this.inputStream = socket.getInputStream();
			this.bufferedInputStream = new BufferedInputStream(this.inputStream);
			this.outputStream = socket.getOutputStream();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 根据请求头信息获取目标接口方法的特定注解
	 *
	 * @param request
	 * @param annoClass TODO
	 * @return
	 * @throws Exception
	 */
	public static <T extends Annotation> T getMethodAnnotation(final ZRequest request, final Class<T> annoClass) {

		final String key = request.getRequestURI() + "@" + annoClass.getCanonicalName();
		final Object v = CACHE_MAP.get(key);
		if (v != null) {
			return (T) v;
		}

		synchronized (key) {

			final T v2 = getMethodAnnotation0(request, annoClass);
			CACHE_MAP.put(key, v2);
			return v2;
		}
	}

	private static <T extends Annotation> T getMethodAnnotation0(final ZRequest request, final Class<T> annoClass) {
		// 匹配path
		if (CU.isEmpty(request.getLineList())) {
			return null;
		}

		final String path = request.getPath();
		final Method method = ZControllerMap.getMethodByMethodEnumAndPath(request.getMethodEnum(), path);
		if (method == null) {

			final Map<String, Method> rowMap = ZControllerMap.getByMethodEnum(request.getMethodEnum());
			final Set<Entry<String, Method>> entrySet = rowMap.entrySet();
			for (final Entry<String, Method> entry : entrySet) {
				final Method methodTarget = entry.getValue();
				final String requestMapping = entry.getKey();
				if (Boolean.TRUE.equals(ZControllerMap.getIsregexByMethodEnumAndPath(methodTarget, requestMapping))
						&& path.matches(requestMapping)) {
					return methodTarget.getAnnotation(annoClass);
				}
			}

			return null;
		}

		return method.getAnnotation(annoClass);
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
		final Method method = ZControllerMap.getMethodByMethodEnumAndPath(request.getMethodEnum(), path);

		// 查找对应的控制器来处理
		if (method == null) {
			return this.handleNoMethodMatche(request, path);
		}

		try {

			final SocketAddress remoteAddress = socketChannel.getRemoteAddress();
			final String ci = remoteAddress.toString();
			final String clientIp = ci.substring(1, ci.indexOf(":"));
			request.setClientIp(clientIp);

			final Object[] p = this.generateParameters(method, request, path, socketChannel);
			if (p == null) {
				return null;
			}

			final Object zController = ZControllerMap.getObjectByMethod(method);
			final ZResponse re = this.invokeAndResponse(method, p, zController, request);
			return re;

		} catch (final Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			this.close();
		}

	}

	private ZResponse handleNoMethodMatche(final ZRequest request,  final String path) throws Exception {
		final Map<MethodEnum, Method> methodMap = ZControllerMap.getByPath(path);
		if (CU.isNotEmpty(methodMap)) {
			final String methodString = methodMap.keySet().stream().map(MethodEnum::getMethod).collect(Collectors.joining(","));
			return new ZResponse(this.socketChannel)
					.header(ZRequest.ALLOW, methodString)
					.httpStatus(HttpStatus.HTTP_405.getCode())
					.contentType(HeaderEnum.APPLICATION_JSON.getType())
					.body(J.toJSONString(CR.error(HttpStatus.HTTP_405.getCode(), "请求Method不支持："
							+ request.getMethodEnum().getMethod() + ", Method: " + methodString), Include.NON_NULL));

		}

		final Map<String, Method> rowMap = ZControllerMap.getByMethodEnum(request.getMethodEnum());
		final Set<Entry<String, Method>> entrySet = rowMap.entrySet();
		for (final Entry<String, Method> entry : entrySet) {
			final Method methodTarget = entry.getValue();
			final String requestMapping = entry.getKey();
			if (Boolean.TRUE.equals(ZControllerMap.getIsregexByMethodEnumAndPath(methodTarget, requestMapping)) &&path.matches(requestMapping)) {

				final Object object = ZControllerMap.getObjectByMethod(methodTarget);
				final Object[] arraygP = this.generateParameters(methodTarget, request, path, this.socketChannel);
				try {
					ZMappingRegex.set(URLDecoder.decode(path, DEFAULT_CHARSET_NAME));
					final ZResponse invokeAndResponse = this.invokeAndResponse(methodTarget, arraygP, object, request);
					return invokeAndResponse;
				} catch (IllegalAccessException | InvocationTargetException | UnsupportedEncodingException e) {
					//					e.printStackTrace();
					// 继续抛出，抛给默认的异常处理器来处理
					throw e;
				}
			}
		}

		// 无匹配的正则表达式接口，返回404
		return	new ZResponse(this.outputStream, this.socketChannel)
				.httpStatus(HttpStatus.HTTP_404.getCode())
				.contentType(DEFAULT_CONTENT_TYPE.getType())
				.body(J.toJSONString(CR.error(HTTP_STATUS_404, "请求方法不存在 [" + path+"]"), Include.NON_NULL))	;
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
			final String message2 = ev.getMessagezf();
			final int x = 1;
			return message2;
		}
		return "";
	}

	private void close() {
		// socketChannel 不关闭
		//		if (this.socketChannel != null) {
		//		}
		if (this.inputStream != null) {
			try {
				this.inputStream.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		if (this.bufferedInputStream != null) {
			try {
				this.bufferedInputStream.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		if (this.outputStream != null) {
			try {
				this.outputStream.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		if (this.socket != null) {
			try {
				this.socket.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("boxing")
	private ZResponse invokeAndResponse(final Method method, final Object[] arraygP, final Object zController, final ZRequest request)
			throws IllegalAccessException, InvocationTargetException, IOException {

		final String controllerName = zController.getClass().getCanonicalName();
		final Integer qps = ZControllerMap.getQPSByControllerNameAndMethodName(controllerName, method.getName());

		final String userAgent = request.getHeader(TaskRequestHandler.USER_AGENT);
		final QPSHandlingEnum handlingEnum = ZContext
				.getBean(RequestValidatorConfigurationProperties.class).getHandlingEnum(userAgent);
		final boolean allow = QC.allow("API" + controllerName + method.getName(), qps, handlingEnum);
		if (!allow) {

			final CR<Object> error = CR.error(AccessDeniedCodeEnum.CLIENT.getCode(),
					AccessDeniedCodeEnum.CLIENT.getMessageToClient());

			final ZResponse response = new ZResponse(this.outputStream, this.socketChannel);
			response.contentType(HeaderEnum.APPLICATION_JSON.getType())
			.httpStatus(HttpStatus.HTTP_429.getCode())
			.body(J.toJSONString(error, Include.NON_NULL));

			return response;
		}

		// 是否超过 ZQPSLimitation.qps
		final ZQPSLimitation zqpsLimitation = ZControllerMap.getZQPSLimitationByControllerNameAndMethodName(controllerName,
				method.getName());
		if (zqpsLimitation != null) {

			switch (zqpsLimitation.type()) {
			case ZSESSIONID:
				final String keyword = controllerName
				+ "@" + method.getName()
				+ "@ZQPSLimitation" + '_'
				+ request.getSession().getId();

				if (!QC.allow(keyword, zqpsLimitation.qps(), handlingEnum)) {

					final CR<Object> error = CR.error(AccessDeniedCodeEnum.ZSESSIONID.getCode(), AccessDeniedCodeEnum.ZSESSIONID.getMessageToClient());
					final ZResponse response = new ZResponse(this.outputStream, this.socketChannel);
					response.contentType(HeaderEnum.APPLICATION_JSON.getType())
					.httpStatus(HttpStatus.HTTP_429.getCode())
					.body(J.toJSONString(error, Include.NON_NULL));
					return response;
				}
				break;

			default:
				break;
			}
		}

		this.setZRequestAndZResponse(arraygP, request);

		Object r = null;
		// 在此zhi执行
		final List<ZHandlerInterceptor> zhiList = ZHandlerInterceptorScanner.match(request.getRequestURI());
		if (CU.isEmpty(zhiList)) {
			r = invoke0(method, arraygP, zController);
		} else {
			final ZResponse response = new ZResponse(this.socketChannel);
			final InterceptorParameter interceptorParameter = new InterceptorParameter(method.getName(), method,
					method.getReturnType().getCanonicalName().equals(Void.class.getCanonicalName()),
					Lists.newArrayList(arraygP), zController);
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

				r = invoke0(method, arraygP, zController);
				final ZModelAndView modelAndView = method.isAnnotationPresent(ZHtml.class)
						? new ZModelAndView(true, String.valueOf(r), readHtmlContent(r), ZModel.get(),
								(ZModel) Arrays.stream(arraygP).filter(arg -> arg.getClass().equals(ZModel.class))
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

		// 接口方法无返回值，直接返回 response对象
		if (Task.VOID.equals(method.getReturnType().getCanonicalName())) {
			final ZResponse response = ZHttpContext.getZResponseAndRemove();
			return response;
		}

		// 响应 html
		if (method.isAnnotationPresent(ZHtml.class)) {
			return this.responseHtml(request, r);
		}

		// 默认响应json
		return this.responseDefault_JSON(request, r);
	}

	/**
	 * 真正的API目标方法执行，统一在本方法里面执行，方便统一处理
	 *
	 * @param method
	 * @param arraygP
	 * @param zController
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws IOException
	 */
	private static Object invoke0(final Method method, final Object[] arraygP, final Object zController)
			throws IllegalAccessException, InvocationTargetException, IOException {

		// FIXME 2024年5月27日 下午1:13:41 zhangzhen: 这个要不要这么写死？或者直接用拦截器算了，定义一个内置的[API方法执行信息]拦截器，并且提供一个开关参数？
		// admin页面要完成的功能有点复杂，包含排序/过滤等，要不要使用derby/h2?
		// FIXME 2024年5月27日 下午3:50:39 zhangzhen: sb_zrepository 已支持sqlite，支持依赖进来使用sqlite吧
		// FIXME 2024年5月29日 下午6:02:36 zhangzhen : 试了还有点问题：
		/*
		 * 以来进来了 sb_zrepository 并且数据源配置为sqlite，单如果A工程依赖了本工程并且也依赖了sb_zrepository，
		 * 则会导致 sb_zrepository 读取数据源时使用A的配置项，而不是本工程配置的sqlite。想好怎么做
		 *
		 * 并且 sb_zrepository 中的log输出也有问题，比如：不希望本工程showsql，而要Ashowsql，zlog2也暂时不支持这么配置。
		 *
		 */


		//		final List<Object> al = Arrays.stream(arraygP)
		//				.filter(a -> a.getClass() != ZRequest.class)
		//				.filter(a -> a.getClass() != ZResponse.class)
		//				.collect(Collectors.toList());

		//		System.out.println("API开始执行,method = " + method.getName() + "\t\t" + "Controller = " + zController.getClass().getSimpleName()
		//				+ "\t" + "arg = " + al
		//				);

		//		final MethodInvocationLogsRepository mr = ZContext.getBean(MethodInvocationLogsRepository.class);
		//
		//		final long t1 = System.currentTimeMillis();

		final Object r = method.invoke(zController, arraygP);

		if (arraygP.length > 0) {
			closeZMFInputStreamAndDeleteTempFile(arraygP);
		}

		//		final long t2 = System.currentTimeMillis();
		//
		//		final MethodInvocationLogsEntity entity = new MethodInvocationLogsEntity();
		//		entity.setMethodName(method.getName());
		//		entity.setTimeConsuming((int) (t2 - t1));
		//		mr.save(entity);

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

	private ZResponse responseDefault_JSON(final ZRequest request, final Object r) {
		final String json = J.toJSONString(r, Include.NON_NULL);
		final ServerConfigurationProperties serverConfiguration = ZSingleton.getSingletonByClass(ServerConfigurationProperties.class);
		if (serverConfiguration.getGzipEnable()
				&& serverConfiguration.gzipContains(DEFAULT_CONTENT_TYPE.getType())
				&& request.isSupportGZIP()) {
			final byte[] compress = ZGzip.compress(json);

			return new ZResponse(this.outputStream, this.socketChannel)
					.header(StaticController.CONTENT_ENCODING, ZRequest.GZIP)
					.contentType(DEFAULT_CONTENT_TYPE.getType()).body(compress);

		}

		return new ZResponse(this.outputStream, this.socketChannel).contentType(DEFAULT_CONTENT_TYPE.getType())
				.body(json);
	}

	private ZResponse responseHtml(final ZRequest request, final Object r) {
		try {

			final String htmlContent = readHtmlContent(r);

			final String html = ZTemplate.freemarker(htmlContent);
			ZModel.clear();

			final ServerConfigurationProperties serverConfiguration = ZSingleton.getSingletonByClass(ServerConfigurationProperties.class);
			if (serverConfiguration.getGzipEnable()
					&& serverConfiguration.gzipContains(HeaderEnum.TEXT_HTML.getType())
					&& request.isSupportGZIP()) {
				final byte[] compress = ZGzip.compress(html);

				return new ZResponse(this.outputStream, this.socketChannel).contentType(HeaderEnum.TEXT_HTML.getType())
						.header(StaticController.CONTENT_ENCODING, ZRequest.GZIP).body(compress);
			}

			return new ZResponse(this.outputStream, this.socketChannel)
					.contentType(HeaderEnum.TEXT_HTML.getType()).body(html);

		} catch (final Exception e) {
			e.printStackTrace();
			return new ZResponse(this.outputStream, this.socketChannel)
					.httpStatus(HttpStatus.HTTP_500.getCode())
					.contentType(DEFAULT_CONTENT_TYPE.getType())
					.body(CR.error(HTTP_STATUS_500 + INTERNAL_SERVER_ERROR));
		}
	}

	private static String readHtmlContent(final Object r) {
		final String ss = String.valueOf(r);
		final String htmlName = ss.charAt(0) == '/' ? ss : '/' + ss;
		final String htmlContent = ResourcesLoader.loadStaticResourceString(htmlName);
		return htmlContent;
	}

	private Object[] generateParameters(final Method method, final Object[] parametersArray, final ZRequest request,
			final String path, final SocketChannel socketChannel) {

		final Parameter[] ps = method.getParameters();
		if (ps.length < parametersArray.length) {
			throw new IllegalArgumentException("方法参数个数小于数组length,method = " + method.getName()
			+ " parametersArray.length = " + parametersArray.length);
		}

		int pI = 0;
		int zpvPI = 0;
		for (final Parameter p : ps) {
			if (p.isAnnotationPresent(ZRequestHeader.class)) {
				final ZRequestHeader a = p.getAnnotation(ZRequestHeader.class);
				final String name = a.value();
				final String headerValue = request.getHeaderMap().get(name);
				if ((headerValue == null) && a.required()) {
					throw new FormPairParseException("请求方法[" + path + "]的header[" + p.getName() + "]不存在");
				}
				parametersArray[pI] = headerValue;
				pI++;
			} else if (p.isAnnotationPresent(ZCookieValue.class)) {
				final ZCookieValue cookieValue = p.getAnnotation(ZCookieValue.class);
				final String cookieName = STU.isEmpty(cookieValue.name()) ? p.getName() : cookieValue.name();
				final ZCookie[] cookies = request.getCookies();
				if (AU.isEmpty(cookies)) {
					if (cookieValue.required()) {
						throw new FormPairParseException("请求方法[" + path + "]缺少名为[" + cookieName + "]的Cookie");
					}
				} else {

					final Optional<ZCookie> c = Arrays.stream(cookies)
							.filter(cookie -> Objects.equals(cookie.getName(), cookieName)).findAny();
					if (c.isPresent()) {
						if (p.getType().getCanonicalName().equals(String.class.getCanonicalName())) {
							parametersArray[pI] = c.get().getValue();
							pI++;
						} else if (p.getType().getCanonicalName().equals(ZCookie.class.getCanonicalName())) {
							parametersArray[pI] = c.get();
							pI++;
						}
					} else {
						if (cookieValue.required()) {
							throw new FormPairParseException("请求方法[" + path + "]缺少名为[" + cookieName + "]的Cookie");
						}
						parametersArray[pI] = null;
						pI++;
					}
				}

			} else if (p.getType().getCanonicalName().equals(ZRequest.class.getCanonicalName())) {
				parametersArray[pI] = request;
				pI++;
			} else if (p.getType().getCanonicalName().equals(ZResponse.class.getCanonicalName())) {
				final ZResponse response = new ZResponse(this.outputStream, this.socketChannel);
				parametersArray[pI] = response;
				pI++;
			} else if (p.getType().getCanonicalName().equals(ZModel.class.getCanonicalName())) {
				final ZModel model = new ZModel();
				parametersArray[pI] = model;
				pI++;
			} else if (p.isAnnotationPresent(ZRequestBody.class)) {
				final byte[] body = request.getBody();
				if (AU.isEmpty(body)) {
					final String simpleName = p.getType().getSimpleName();
					throw new FormPairParseException("@" + ZRequestBody.class.getSimpleName() + " 参数 " + simpleName + " 不存在");
				}

				final Object object = J.parseObject(new String(body), p.getType());
				if (object == null) {
					final String simpleName = p.getType().getSimpleName();
					throw new FormPairParseException("@" + ZRequestBody.class.getSimpleName() + " 参数 " + simpleName + " 错误");
				}

				Task.checkZValidated(p, object);

				parametersArray[pI] = object;
				pI++;

			} else if (p.isAnnotationPresent(ZRequestParam.class)) {
				pI = Task.hZRequestParam(parametersArray, request, path, pI, p);
			} else if (p.isAnnotationPresent(ZPathVariable.class)) {
				final List<Object> list = ZPVTL.get();
				final Class<?> type = p.getType();
				// FIXME 2023年11月8日 下午4:39:18 zhanghen: @ZRM 启动校验是否此类型
				try {
					final Object a = list.get(zpvPI);
					Task.setZPathVariableValue(parametersArray, pI, type, a);
					zpvPI++;


					// FIXME 2023年11月8日 下午10:47:54 zhanghen: TODO 继续支持 校验注解
					if (p.isAnnotationPresent(ZPositive.class)) {
						ZValidator.validatedZPositive(p, parametersArray[pI]);
					}
					if (p.isAnnotationPresent(ZMin.class)) {
						ZValidator.validatedZMin(p, parametersArray[pI],p.getAnnotation(ZMin.class).min());
					}


				} catch (final Exception e) {
					e.printStackTrace();
					final String causedby = ZControllerAdviceThrowable.findCausedby(e);
					throw new PathVariableException(causedby);
				}

				pI++;
			} else if (p.getType().getCanonicalName().equals(ZMultipartFile.class.getCanonicalName())) {
				if (AU.isEmpty(request.getOriginalRequestBytes())) {
					throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在");
				}

				final List<FD2> fdList = BodyReader.readFormDate(request.getOriginalRequestBytes(),
						request.getContentType(), request.getBoundary());
				final Optional<FD2> findAny = fdList.stream().filter(fd -> fd.getName().equals(p.getName())).findAny();
				if (findAny.isPresent()) {

					final InputStream inputStream = new ByteArrayInputStream(findAny.get().getBody());
					final ZMultipartFile file = new ZMultipartFile(findAny.get().getName(),
							null,
							findAny.get().getFileName(),
							findAny.get().getBody(),
							false,
							findAny.get().getContentType(),
							inputStream);

					pI = Task.setValue(parametersArray, pI, p, file);

				} else {

					if (!p.getName().equals(request.getTf().getName())) {
						throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在");
					}

					InputStream inputStream = null;
					try {
						inputStream = new FileInputStream(request.getTf().getFile());
					} catch (final FileNotFoundException e) {
						e.printStackTrace();
					}

					final String contentType = request.getTf().getContentType();

					final ZMultipartFile file = new ZMultipartFile (request.getTf().getName(),
							request.getTf().getTempFilePath(),
							request.getTf().getFileName(),
							null,
							true, contentType, inputStream);

					pI = Task.setValue(parametersArray, pI, p, file);
				}

			}

		}

		return parametersArray;
	}

	/**
	 * @ZPathVariable 支持的类型
	 */
	public final static ImmutableSet<String> ZPV_TYPE = ImmutableSet.copyOf(Lists.newArrayList(
			Byte.class.getCanonicalName(), Short.class.getCanonicalName(), Integer.class.getCanonicalName(),
			Long.class.getCanonicalName(), Float.class.getCanonicalName(), Double.class.getCanonicalName(),
			Boolean.class.getCanonicalName(), Character.class.getCanonicalName(), String.class.getCanonicalName()));

	private static void setZPathVariableValue(final Object[] parametersArray, final int pI, final Class<?> type, final Object a) {
		if (type.getCanonicalName().equals(Byte.class.getCanonicalName())) {
			parametersArray[pI] = Byte.valueOf(String.valueOf(a));
		} else if (type.getCanonicalName().equals(Short.class.getCanonicalName())) {
			parametersArray[pI] = Short.valueOf(String.valueOf(a));
		} else if (type.getCanonicalName().equals(Integer.class.getCanonicalName())) {
			parametersArray[pI] = Integer.valueOf(String.valueOf(a));
		} else if (type.getCanonicalName().equals(Long.class.getCanonicalName())) {
			parametersArray[pI] = Long.valueOf(String.valueOf(a));
		} else if (type.getCanonicalName().equals(Float.class.getCanonicalName())) {
			parametersArray[pI] = Float.valueOf(String.valueOf(a));
		} else if (type.getCanonicalName().equals(Double.class.getCanonicalName())) {
			parametersArray[pI] = Double.valueOf(String.valueOf(a));
		} else if (type.getCanonicalName().equals(Boolean.class.getCanonicalName())) {
			parametersArray[pI] = Boolean.valueOf(String.valueOf(a));
		} else if (type.getCanonicalName().equals(Character.class.getCanonicalName())) {
			parametersArray[pI] = Character.valueOf(String.valueOf(a).charAt(0));
		}

		else if (type.getCanonicalName().equals(String.class.getCanonicalName())) {
			parametersArray[pI] = String.valueOf(a);
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

				final String defaultValue = p.getAnnotation(ZRequestParam.class).defaultValue();
				if (defaultValue != null) {
					try {
						piR = Task.setValue(parametersArray, pI, p, defaultValue);
					} catch (final Exception e) {
						throw new FormPairParseException(p.getName() + " = " + defaultValue);
					}
					return piR;
				}

				throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在");
			}

			piR = Task.setValue(parametersArray, pI, p, findAny.get().getValue());
		} else {
			final byte[] body = request.getBody();
			if (AU.isEmpty(body)) {
				final String defaultValue = p.getAnnotation(ZRequestParam.class).defaultValue();
				if (defaultValue != null) {
					try {
						piR = Task.setValue(parametersArray, pI, p, defaultValue);
					} catch (final Exception e) {
						throw new FormPairParseException(p.getName() + " = " + defaultValue);
					}
					return piR;
				}
				throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在");
			}

			final List<FD2> fdList = BodyReader.readFormDate(request.getOriginalRequestBytes(),
					request.getContentType(), request.getBoundary());
			if (CU.isEmpty(fdList)) {
				throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在");
			}

			final Optional<FD2> findAny = fdList.stream()
					// FIXME 2024年12月21日 下午10:05:21 zhangzhen : 这个是isEmpty？是当时手误写错了？记得debug看下
					.filter(f -> STU.isEmpty(f.getFileName()))
					.filter(f -> f.getName().equals(p.getName()))
					.findAny();
			if (!findAny.isPresent()) {
				throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在");
			}

			piR = Task.setValue(parametersArray, pI, p, findAny.get().getValue());
		}
		return piR;
	}

	private static void checkZValidated(final Parameter p, final Object object) {
		if (!p.isAnnotationPresent(ZValidated.class)) {
			return;
		}

		final ArrayList<Class<?>> pl = Lists.newArrayList(object.getClass());
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

	private static int setValue(final Object[] parametersArray, final int pI, final Parameter p, final Object value) {

		final AtomicInteger nI = new AtomicInteger(pI);
		if (p.getType().getCanonicalName().equals(Byte.class.getCanonicalName())) {
			parametersArray[nI.getAndIncrement()] = Byte.valueOf(String.valueOf(value));
		} else if (p.getType().getCanonicalName().equals(Short.class.getCanonicalName())) {
			parametersArray[nI.getAndIncrement()] = Short.valueOf(String.valueOf(value));
		} else if (p.getType().getCanonicalName().equals(Integer.class.getCanonicalName())) {
			parametersArray[nI.getAndIncrement()] = Integer.valueOf(String.valueOf(value));
		} else if (p.getType().getCanonicalName().equals(Long.class.getCanonicalName())) {
			parametersArray[nI.getAndIncrement()] = Long.valueOf(String.valueOf(value));
		} else if (p.getType().getCanonicalName().equals(Float.class.getCanonicalName())) {
			parametersArray[nI.getAndIncrement()] = Float.valueOf(String.valueOf(value));
		} else if (p.getType().getCanonicalName().equals(Double.class.getCanonicalName())) {
			parametersArray[nI.getAndIncrement()] = Double.valueOf(String.valueOf(value));
		} else if (p.getType().getCanonicalName().equals(Character.class.getCanonicalName())) {
			parametersArray[nI.getAndIncrement()] = Character.valueOf(String.valueOf(value).charAt(0));
		} else if (p.getType().getCanonicalName().equals(Boolean.class.getCanonicalName())) {
			parametersArray[nI.getAndIncrement()] = Boolean.valueOf(String.valueOf(value));
		} else {
			parametersArray[nI.getAndIncrement()] = value;
		}

		return nI.get();
	}

	private Object[] generateParameters(final Method method, final ZRequest request, final String path,
			final SocketChannel socketChannel) {
		final Object[] parametersArray = new Object[method.getParameters().length];

		return this.generateParameters(method, parametersArray, request, path, socketChannel);
	}

	private void setZRequestAndZResponse(final Object[] arraygP, final ZRequest request) {

		if (arraygP == null) {
			return;
		}

		ZHttpContext.setZRequest(request);

		boolean sR = false;
		for (final Object object : arraygP) {
			if (object == null) {
				continue;
			}

			if (ZResponse.class.getCanonicalName().equals(object.getClass().getCanonicalName())) {
				ZHttpContext.setZResponse((ZResponse) object);
				sR = true;
				break;
			}
		}

		if (!sR) {
			ZHttpContext.setZResponse(new ZResponse(this.outputStream, this.socketChannel));
		}
	}

}
