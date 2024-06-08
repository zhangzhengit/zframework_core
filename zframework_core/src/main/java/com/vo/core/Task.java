package com.vo.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import com.google.common.collect.Sets;
import com.vo.anno.ZCookieValue;
import com.vo.anno.ZRequestBody;
import com.vo.anno.ZRequestHeader;
import com.vo.aop.InterceptorParameter;
import com.vo.api.StaticController;
import com.vo.cache.J;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.core.ZRequest.RequestLine;
import com.vo.core.ZRequest.RequestParam;
import com.vo.enums.MethodEnum;
import com.vo.exception.FormPairParseException;
import com.vo.exception.PathVariableException;
import com.vo.exception.ZControllerAdviceThrowable;
import com.vo.html.ResourcesLoader;
import com.vo.http.HttpStatus;
import com.vo.http.LineMap;
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
import com.vo.validator.ZMin;
import com.vo.validator.ZPositive;
import com.vo.validator.ZValidated;
import com.vo.validator.ZValidator;
import com.votool.common.CR;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年7月3日
 *
 */
public class Task {

	private static final int DEFAULT_BUFFER_SIZE = 8192;
	private static final int READ_LENGTH = DEFAULT_BUFFER_SIZE / 2;
	public static final String SP = "&";
	public static final String EMPTY_STRING = "";

	public static final String DEFAULT_CHARSET_NAME = Charset.defaultCharset().displayName();
	public static final String VOID = "void";
	public static final Charset DEFAULT_CHARSET = Charset.defaultCharset();
	public static final String HTTP_200 = "HTTP/1.1 200";
	public static final int HTTP_STATUS_500 = 500;
	public static final int HTTP_STATUS_404 = 404;
	public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
	public static final HeaderEnum DEFAULT_CONTENT_TYPE = HeaderEnum.JSON;
	public static final String NEW_LINE = "\r\n";
	private static final Map<Object, Object> CACHE_MAP = new WeakHashMap<>(1024, 1F);

	private static final ThreadLocal<SocketChannel> SCTL = new ThreadLocal<>();
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

	public ZRequest handleRead(final String requestString) {
		final ZRequest request = new ZRequest();

		final boolean contains = requestString.contains(NEW_LINE);
		if (contains) {
			final String[] aa = requestString.split(NEW_LINE);
			for (final String string : aa) {
				request.addLine(string);
			}
		}

		final ZRequest parseRequest = Task.parseRequest(request);
		return parseRequest;
	}

	/**
	 * 执行目标方法（接口Method）
	 *
	 * @param request 请求体
	 * @return 响应结果，已根据具体的方法处理好header、cookie、body等内容，只是没write
	 * @throws Exception
	 *
	 */
	public ZResponse invoke(final ZRequest request) throws Exception {
		// 匹配path
		final RequestLine requestLine = request.getRequestLine();
		if (CollUtil.isEmpty(request.getLineList())) {
			return null;
		}

		final String path = requestLine.getPath();
		final Method method = ZControllerMap.getMethodByMethodEnumAndPath(requestLine.getMethodEnum(), path);

		// 查找对应的控制器来处理
		if (method == null) {
			return this.handleNoMethodMatche(request, requestLine, path);
		}

		try {
			final Object[] p = this.generateParameters(method, request, requestLine, path);
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

	private ZResponse handleNoMethodMatche(final ZRequest request, final RequestLine requestLine, final String path) throws Exception {
		final Map<MethodEnum, Method> methodMap = ZControllerMap.getByPath(path);
		if (CollUtil.isNotEmpty(methodMap)) {
			final String methodString = methodMap.keySet().stream().map(MethodEnum::getMethod).collect(Collectors.joining(","));
			return new ZResponse(this.socketChannel)
					.header(ZRequest.ALLOW, methodString)
					.httpStatus(HttpStatus.HTTP_405.getCode())
					.contentType(HeaderEnum.JSON.getType())
					.body(J.toJSONString(CR.error(HttpStatus.HTTP_405.getCode(), "请求Method不支持："
							+ requestLine.getMethodEnum().getMethod() + ", Method: " + methodString), Include.NON_NULL));

		}

		final Map<String, Method> rowMap = ZControllerMap.getByMethodEnum(requestLine.getMethodEnum());
		final Set<Entry<String, Method>> entrySet = rowMap.entrySet();
		for (final Entry<String, Method> entry : entrySet) {
			final Method methodTarget = entry.getValue();
			final String requestMapping = entry.getKey();
			if (Boolean.TRUE.equals(ZControllerMap.getIsregexByMethodEnumAndPath(methodTarget, requestMapping)) &&path.matches(requestMapping)) {

				final Object object = ZControllerMap.getObjectByMethod(methodTarget);
				final Object[] arraygP = this.generateParameters(methodTarget, request, requestLine, path);
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

		final String eMessage = stringWriter.toString();

		return eMessage;
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
			throws IllegalAccessException, InvocationTargetException {


		final String controllerName = zController.getClass().getCanonicalName();
		final Integer qps = ZControllerMap.getQPSByControllerNameAndMethodName(controllerName, method.getName());
		// 是否超过 ZRequestMapping.qps
		if (!QPSCounter.allow("API" + controllerName + method.getName(), qps, QPSEnum.API_METHOD)) {

			final String message = "访问频繁，请稍后再试";

			final ZResponse response = new ZResponse(this.outputStream, this.socketChannel);
			response.contentType(HeaderEnum.JSON.getType())
			.httpStatus(HttpStatus.HTTP_403.getCode())
			.body(J.toJSONString(CR.error(message), Include.NON_NULL));

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

				// FIXME 2024年5月3日 下午8:01:22 zhangzhen: 这是很早之前写的，现在做其他的发现了传为null导致bug，
				// 先用QPSEnum.API_METHOD，以后再看是否合适
				if (!QPSCounter.allow(keyword, zqpsLimitation.qps(), QPSEnum.API_METHOD)) {

					final String message = "接口访问频繁，请稍后再试";

					final ZResponse response = new ZResponse(this.outputStream, this.socketChannel);
					response.contentType(HeaderEnum.JSON.getType())
					.httpStatus(HttpStatus.HTTP_403.getCode())
					.body(J.toJSONString(CR.error(message), Include.NON_NULL));
					return response;
				}
				break;

			default:
				break;
			}
		}

		this.setZRequestAndZResponse(arraygP, request);

		// 接口方法无返回值，直接返回 response对象
		if (Task.VOID.equals(method.getReturnType().getCanonicalName())) {
			invoke0(method, arraygP, zController);
			final ZResponse response = ZHttpContext.getZResponseAndRemove();
			return response;
		}

		Object r = null;
		// 在此zhi执行
		final List<ZHandlerInterceptor> zhiList = ZHandlerInterceptorScanner.match(request.getRequestURI());
		if (CollUtil.isEmpty(zhiList)) {
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

		// 响应
		if (method.isAnnotationPresent(ZHtml.class)) {
			return this.responseHtml(request, r);
		}

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
	 */
	private static Object invoke0(final Method method, final Object[] arraygP, final Object zController)
			throws IllegalAccessException, InvocationTargetException {

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


		final List<Object> al = Arrays.stream(arraygP)
				.filter(a -> a.getClass() != ZRequest.class)
				.filter(a -> a.getClass() != ZResponse.class)
				.collect(Collectors.toList());

		System.out.println("API开始执行,method = " + method.getName() + "\t\t" + "Controller = " + zController.getClass().getSimpleName()
				+ "\t" + "arg = " + al
				);

		//		final MethodInvocationLogsRepository mr = ZContext.getBean(MethodInvocationLogsRepository.class);
		//
		//		final long t1 = System.currentTimeMillis();

		final Object r = method.invoke(zController, arraygP);
		//		final long t2 = System.currentTimeMillis();
		//
		//		final MethodInvocationLogsEntity entity = new MethodInvocationLogsEntity();
		//		entity.setMethodName(method.getName());
		//		entity.setTimeConsuming((int) (t2 - t1));
		//		mr.save(entity);

		return r;
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
					&& serverConfiguration.gzipContains(HeaderEnum.HTML.getType())
					&& request.isSupportGZIP()) {
				final byte[] compress = ZGzip.compress(html);

				return new ZResponse(this.outputStream, this.socketChannel).contentType(HeaderEnum.HTML.getType())
						.header(StaticController.CONTENT_ENCODING, ZRequest.GZIP).body(compress);
			}

			return new ZResponse(this.outputStream, this.socketChannel)
					.contentType(HeaderEnum.HTML.getType()).body(html);

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
			final RequestLine requestLine, final String path) {

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
				final String headerValue = requestLine.getHeaderMap().get(name);
				if ((headerValue == null) && a.required()) {
					throw new FormPairParseException("请求方法[" + path + "]的header[" + p.getName() + "]不存在");
				}
				parametersArray[pI] = headerValue;
				pI++;
			} else if (p.isAnnotationPresent(ZCookieValue.class)) {
				final ZCookieValue cookieValue = p.getAnnotation(ZCookieValue.class);
				final String cookieName = StrUtil.isEmpty(cookieValue.name()) ? p.getName() : cookieValue.name();
				final ZCookie[] cookies = request.getCookies();
				if (ArrayUtil.isEmpty(cookies)) {
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
				final String body = request.getBody();
				if (StrUtil.isEmpty(body)) {
					final String simpleName = p.getType().getSimpleName();
					throw new FormPairParseException("@" + ZRequestBody.class.getSimpleName() + " 参数 " + simpleName + " 不存在");
				}

				final Object object = J.parseObject(body, p.getType());
				if (object == null) {
					final String simpleName = p.getType().getSimpleName();
					throw new FormPairParseException("@" + ZRequestBody.class.getSimpleName() + " 参数 " + simpleName + " 错误");
				}

				Task.checkZValidated(p, object);

				parametersArray[pI] = object;
				pI++;

			} else if (p.isAnnotationPresent(ZRequestParam.class)) {
				pI = this.hZRequestParam(parametersArray, request, requestLine, path, pI, p);
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
					// NumberFormatException
					//					final String message = Task.gExceptionMessage(e);
					final String causedby = ZControllerAdviceThrowable.findCausedby(e);
					throw new PathVariableException(causedby);
				}

				pI++;
			} else if (p.getType().getCanonicalName().equals(ZMultipartFile.class.getCanonicalName())) {
				// FIXME 2023年10月26日 下午9:28:39 zhanghen: 写这里
				final String body = request.getBody();
				if (StrUtil.isEmpty(body)) {
					throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在");
				}
				final byte[] originalRequestBytes = request.getOriginalRequestBytes();

				final List<FormData> fdList = FormData.parseFormData(originalRequestBytes);
				if (CollUtil.isEmpty(fdList)) {
					throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在");
				}

				final Optional<FormData> findAny = fdList.stream()
						.filter(f -> StrUtil.isNotEmpty(f.getFileName()))
						.filter(f -> f.getName().equals(p.getName()))
						.findAny();
				if (!findAny.isPresent()) {
					throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在");
				}

				final ZMultipartFile file = new ZMultipartFile (findAny.get().getName(),
						findAny.get().getFileName(),
						findAny.get().getValue().getBytes(NioLongConnectionServer.CHARSET),
						findAny.get().getContentType(), null);

				pI = Task.setValue(parametersArray, pI, p, file);
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

	private int hZRequestParam(final Object[] parametersArray, final ZRequest request, final RequestLine requestLine,
			final String path, final int pI, final Parameter p) {

		int piR = 0;
		final Set<RequestParam> paramSet = requestLine.getParamSet();
		if (CollUtil.isNotEmpty(paramSet)) {
			final Optional<RequestParam> findAny = paramSet.stream()
					.filter(rp -> rp.getName().equals(p.getName()))
					.findAny();
			if (!findAny.isPresent()) {
				throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在");
			}

			piR = Task.setValue(parametersArray, pI, p, findAny.get().getValue());
		} else {
			final String body = request.getBody();
			if (StrUtil.isEmpty(body)) {
				throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在");
			}
			final byte[] originalRequestBytes = request.getOriginalRequestBytes();

			final List<FormData> fdList = FormData.parseFormData(originalRequestBytes);
			if (CollUtil.isEmpty(fdList)) {
				throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在");
			}

			final Optional<FormData> findAny = fdList.stream()
					.filter(f -> StrUtil.isEmpty(f.getFileName()))
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

		final Class<?> type = p.getType();
		final Field[] fields = type.getDeclaredFields();
		for (final Field field : fields) {
			ZValidator.validatedAll(object, field);
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

	private Object[] generateParameters(final Method method, final ZRequest request, final RequestLine requestLine, final String path) {
		final Object[] parametersArray = new Object[method.getParameters().length];

		return this.generateParameters(method, parametersArray, request, requestLine, path);
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


	public static ZRequest parseRequest(final ZRequest request) {

		final Object v = CACHE_MAP.get(request);
		if (v != null) {
			return (ZRequest) v;
		}

		synchronized (request) {
			final ZRequest v2 = parseRequest0(request);
			if (v2 == null) {
				return v2;
			}

			CACHE_MAP.put(request, v2);
			return v2;
		}

	}

	private static ZRequest parseRequest0(final ZRequest request) {
		final ZRequest.RequestLine requestLine = new ZRequest.RequestLine();
		if (CollUtil.isEmpty(request.getLineList())) {
			return request;
		}

		// 0 为 请求行
		final String line = request.getLineList().get(0);
		requestLine.setOriginal(line);

		// method
		final int methodIndex = line.indexOf(" ");
		if (methodIndex > -1) {
			final String methodS = line.substring(0, methodIndex);
			final MethodEnum me = MethodEnum.valueOfString(methodS);
			// 可能是null，在这里不管，在外面处理，返回405
			requestLine.setMethodEnum(me);
		}

		// path
		parsePath(line, requestLine, methodIndex);

		// version
		parseVersion(requestLine, line);

		LineMap.put(requestLine.getFullpath(), requestLine);

		// paserHeader
		paserHeader(request, requestLine);

		// parseBody
		parseBody(request, requestLine);

		// clientIp
		request.setClientIp(getClientIp());

		request.setRequestLine(requestLine);

		return request;
	}

	private static String getClientIp() {

		final SocketChannel socketChannel = SCTL.get();
		if (socketChannel == null) {
			return null;
		}
		// FIXME 2023年11月16日 下午2:47:38 zhanghen: ab 测试这里可能取不到,修复掉
		final Socket socket = socketChannel.socket();
		if (socket == null) {
			return null;
		}
		final InetSocketAddress inetSocketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
		if (inetSocketAddress == null) {
			return null;
		}

		final InetAddress address = inetSocketAddress.getAddress();
		return address.getHostAddress();
	}

	private static void parsePath(final String s, final ZRequest.RequestLine line, final int methodIndex) {
		final int pathI = s.indexOf(" ", methodIndex + 1);
		if (pathI > methodIndex) {
			final String fullPath = s.substring(methodIndex  + 1, pathI);

			final int wenI = fullPath.indexOf("?");
			if (wenI > -1) {
				line.setQueryString(fullPath.substring(("?".length() + wenI) - 1));

				final Set<RequestParam> paramSet = Sets.newHashSet();
				final String param = fullPath.substring("?".length() + wenI);
				final String simplePath = fullPath.substring(0,wenI);

				try {
					line.setPath(java.net.URLDecoder.decode(simplePath, DEFAULT_CHARSET_NAME));
				} catch (final UnsupportedEncodingException e) {
					e.printStackTrace();
				}

				final String[] paramArray = param.split(SP);
				for (final String p : paramArray) {
					final String[] p0 = p.split("=");
					final ZRequest.RequestParam requestParam = new ZRequest.RequestParam();
					requestParam.setName(p0[0]);
					if (p0.length >= 2) {
						try {
							final String v = StrUtil.isEmpty(p0[1]) ? EMPTY_STRING
									: java.net.URLDecoder.decode(p0[1], DEFAULT_CHARSET_NAME);
							requestParam.setValue(v);
						} catch (final UnsupportedEncodingException e) {
							e.printStackTrace();
						}
					} else {
						requestParam.setValue(EMPTY_STRING);
					}

					paramSet.add(requestParam);
				}

				line.setParamSet(paramSet);

			} else {
				try {
					line.setPath(java.net.URLDecoder.decode(fullPath, DEFAULT_CHARSET_NAME));
				} catch (final UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			line.setFullpath(fullPath);
		}
	}

	private static void parseVersion(final ZRequest.RequestLine requestLine, final String line) {
		final int hI = line.lastIndexOf("HTTP/");
		if (hI > -1) {
			final String version = line.substring(hI);
			requestLine.setVersion(version);
		}
	}

	private static void paserHeader(final ZRequest request, final ZRequest.RequestLine requestLine) {
		final List<String> x = request.getLineList();
		final HashMap<String, String> hm = new HashMap<>(16, 1F);
		for (int i = x.size() - 1; i > 0; i--) {
			final String l = x.get(i);
			if (EMPTY_STRING.equals(l)) {
				continue;
			}

			final int k = l.indexOf(":");
			if (k > -1) {
				final String key = l.substring(0, k).trim();
				final String value = l.substring(k + 1).trim();

				hm.put(key, value);
			}
		}

		requestLine.setHeaderMap(hm);
	}

	private static void parseBody(final ZRequest request, final ZRequest.RequestLine requestLine) {
		final List<String> x = request.getLineList();
		for (int i = 1; i < x.size(); i++) {
			final String l2 = x.get(i);
			if (EMPTY_STRING.equals(l2) && (i < x.size()) && ((i + 1) < x.size())) {

				final String contentType = requestLine.getHeaderMap().get(ZRequest.CONTENT_TYPE);

				//				System.out.println("contentType = " + contentType);

				if (contentType.equalsIgnoreCase(HeaderEnum.JSON.getType())
						|| contentType.toLowerCase().contains(HeaderEnum.JSON.getType().toLowerCase())) {

					final StringBuilder json = new StringBuilder();
					for (int k = i + 1; k < x.size(); k++) {
						json.append(x.get(k));
					}
					request.setBody(json.toString());
				} else if (contentType.equalsIgnoreCase(HeaderEnum.URLENCODED.getType())
						|| contentType.toLowerCase().contains(HeaderEnum.URLENCODED.getType().toLowerCase())) {

					//					System.out.println("contentType = " + contentType);
					System.out.println("OKapplication/x-www-form-urlencoded");
					// id=200&name=zhangsan 格式

					final StringBuilder formBu = new StringBuilder();
					for (int k = i + 1; k < x.size(); k++) {
						formBu.append(x.get(k));
					}

					if (formBu.length() > 0) {
						request.setBody(formBu.toString());

						System.out.println("from = " + formBu);
						final String fr = formBu.toString();
						final String[] fA = fr.split("&");

						for (final String v : fA) {
							final String[] vA = v.split("=");
						}

					}

					// FORM_DATA 用getType
				} else if (contentType.toLowerCase().startsWith(HeaderEnum.FORM_DATA.getType().toLowerCase())) {

					// FIXME 2023年8月11日 下午10:19:34 zhanghen: TODO 继续支持 multipart/form-data
					//					System.out.println("okContent-Type: multipart/form-data");

					final ArrayList<String> body = Lists.newArrayList();
					final StringBuilder formBu = new StringBuilder();
					for (int k = i + 1; k < x.size(); k++) {
						formBu.append(x.get(k)).append(Task.NEW_LINE);
						body.add(x.get(k));
					}


					request.setBody(formBu.toString());
					//					System.out.println("formBu = \n" + formBu);

					final List<FormData> formList = FormData.parseFormData(body.toArray(new String[0]), body.get(0));
					//					System.out.println("---------formList.size = " + formList.size());
					for (final FormData form: formList) {
						//						System.out.println(form);
					}
					//					System.out.println("---------formList.size = " + formList.size());
				}


				break;
			}

		}
	}

	public ZRequest readAndParse() {
		final ZRequest r1 = this.read();
		final ZRequest r2 = this.parse(r1);
		return r2;
	}

	public ZRequest read() {
		final ZRequest request = this.handleRead();
		return request;
	}

	private ZRequest handleRead() {

		final ZRequest request = new ZRequest();

		final int nk = READ_LENGTH;
		final List<Byte> list = new ArrayList<>(nk);

		while (true) {
			final byte[] bs = new byte[nk];
			int read = -4;
			try {
				read = this.bufferedInputStream.read(bs);
			} catch (final IOException e) {
				//						e.printStackTrace();
				break;
			}
			if (read <= 0) {
				break;
			}

			for (int i = 0; i < read; i++) {
				list.add(bs[i]);
			}
			if (read <= nk) {
				break;
			}
		}

		final byte[] bsR = new byte[list.size()];
		for (int x = 0; x < list.size(); x++) {
			bsR[x] = list.get(x);
		}

		final String r = new String(bsR);

		final boolean contains = r.contains(NEW_LINE);
		if (contains) {
			final String[] aa = r.split(NEW_LINE);
			for (final String string : aa) {
				request.addLine(string);
			}
		}

		//					bufferedInputStream.close();
		//					inputStream.close();

		return request;

	}

	public ZRequest parse(final ZRequest request) {
		return Task.parseRequest(request);
	}

}
