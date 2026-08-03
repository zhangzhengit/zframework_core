package vo.vortex.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import vo.vortex.anno.ZCacheControl;
import vo.vortex.anno.ZController;
import vo.vortex.anno.ZETag;
import vo.vortex.anno.ZQPSLimitation;
import vo.vortex.anno.ZRequestMapping;
import vo.vortex.common.CR;
import vo.vortex.common.CU;
import vo.vortex.common.J;
import vo.vortex.common.STU;
import vo.vortex.compression.CF;
import vo.vortex.configuration.properties.ServerConfigurationProperties;
import vo.vortex.core.ZContext;
import vo.vortex.enums.AcceptEncodingEnum;
import vo.vortex.enums.CacheControlEnum;
import vo.vortex.enums.ContentTypeEnum;
import vo.vortex.enums.HeaderEnum;
import vo.vortex.enums.HttpStatusEnum;
import vo.vortex.enums.ZQPSLimitationEnum;
import vo.vortex.html.FIS;
import vo.vortex.html.ResourcesLoader;
import vo.vortex.http.request.ZRequest;
import vo.vortex.http.response.ZResponse;

/**
 *
 * 内置的处理静态资源的接口，处理比如 .css .js .jpg 等文件
 *
 * @author zhangzhen
 * @date 2023年6月28日
 *
 */
@ZController
public class StaticController {

	private static final ServerConfigurationProperties SERVER_CONFIGURATION = ZContext.getBean(ServerConfigurationProperties.class);

	private static final Set<String> R_D_S = new CopyOnWriteArraySet<>();

	// FIXME 2026年6月25日 11:39:29 zhangzhen : -jar运行时resources下目录就用不到了br预压缩了，
	//　要不要提前把resources下的目录复制出来(和app.p一样)放在一个目录（用配置项），然后和server.static.path指定的
	// 目录一样，就可以用br预压缩了

	// FIXME 2026年6月24日 14:16:26 zhangzhen : 要不要加入"/.+\\..+$"支持A.B的形式来匹配所有的静态资源请求，并加一个配置项是否启用此mapping？
	@ZRequestMapping(mapping = { "/favicon\\.ico",
			"/.+\\.java$",
			"/.+\\.woff$",
			"/.+\\.csv$",
			"/.+\\.json$",
			"/.+\\.ttf$",
			"/.+\\.log$",
			"/.+\\.sql$",
			"/.+\\.txt$",
			"/.+\\.png$",
			"/.+\\.wav$",
			"/.+\\.js$", "/.+\\.jpg$", "/.+\\.mp3$", "/.+\\.mp4$", "/.+\\.pdf$",
			"/.+\\.gif$", "/.+\\.doc$", "/.+\\.css$", "/.+\\.html$" }
		, isRegex = { true, true, true, true, true, true, true, true, true,true,true,true,
					true,true, true, true, true, true, true, true},
				count = 10000 * 10)

	@ZETag
	@ZQPSLimitation(count = 2000, type = ZQPSLimitationEnum.ZSESSIONID)
	@ZCacheControl(value = { CacheControlEnum.PUBLIC, CacheControlEnum.MUST_REVALIDATE }, maxAge = 60 * 10)
	public void staticResources(final ZResponse response, final ZRequest request) {

		if (!checkReferer(request)) {
			httpStatus403(response);
			return;
		}

		final String resourceName = request.getRequestURI();

		final int i = resourceName.lastIndexOf(".");
		if (i <= -1) {
			response.httpStatus(HttpStatusEnum.HTTP_500.getStatus())
					.contentType(ContentTypeEnum.APPLICATION_JSON.getTypeBytes())
					.body(J.toJSONString(CR.error("不支持无后缀的文件")));
			return;
		}

		final int wi = resourceName.lastIndexOf("?");
		String sn = null;

		String resourceName2 = null;
		if (wi > -1) {
			final int i2 = resourceName.indexOf(".", 0);
			sn = resourceName.substring(i2 + 1, wi);
			resourceName2 = resourceName.substring(0, wi);
		} else {
			sn = resourceName.substring(i + 1, wi > -1 ? wi : resourceName.length());
			resourceName2 = resourceName;
		}

		final Map<String, String> ctm = SERVER_CONFIGURATION.getStaticControllerContentType();
		final String ct = ctm.get(sn);
		if (STU.isEmpty(ct)) {
			response.httpStatus(HttpStatusEnum.HTTP_500.getStatus())
					.contentType(ContentTypeEnum.APPLICATION_JSON.getTypeBytes())
					.body(J.toJSONString(CR.error("不支持的文件类型")));
			return;
		}

		response.contentType(ct);

		if (R_D_S.contains(resourceName2)) {
			final byte[] data = ResourcesLoader.loadStaticResourceAsByteArray(resourceName2);
			response.body(data);
			return;
		}

		final FIS fis = ResourcesLoader.loadStaticResourceAsInputStream(resourceName2);
		responseBody(response, request, fis, resourceName2);
	}

	private static void responseBody(final ZResponse response, final ZRequest request, final FIS sourceFis, final String resourceName) {

		if (sourceFis.getFile() != null) {

			final File file = sourceFis.getFile();
			// 文件不大于缓存阈值，则直接读入内存并且放入缓存然后直接响应了，就不使用文件流读写了
			if (file.length() <= ResourcesLoader.StaticResourceCacheSize_BYTE) {
				final byte[] data = ResourcesLoader.loadStaticResourceAsByteArray(resourceName);
				response.body(data);
				R_D_S.add(resourceName);
				return;
			}

			// 一次找出所有的压缩文件和原文件，并且按文件大小从小到大排序，四选一优先响应小的
			final List<CF> cfl = StaticResourcesPreCompressionService.gCFOrderByFileLength(sourceFis.getFile());

			for (final CF cf : cfl) {
				final AcceptEncodingEnum ae = cf.getAcceptEncodingEnum();
				if (((ae == AcceptEncodingEnum.BR)   && request.isSupportBR())
				||  ((ae == AcceptEncodingEnum.ZSTD) && request.isSupportZSTD())
				||  ((ae == AcceptEncodingEnum.GZIP) && request.isSupportGZIP())
				||  ((ae == null))) {
					rb(response, cf.getFile(), ae);
					// 执行到此一次就return
					return;
				}
			}
		}

		// 响应原始文件
		response.body(sourceFis);
		return;
	}

	private static void rb(final ZResponse response, final File file, final AcceptEncodingEnum acceptEncodingEnum) {
		try (final InputStream inputStream = Files.newInputStream(Paths.get(file.getAbsolutePath()))) {
			final FIS fis = new FIS(inputStream, file);
			fis.setAcceptEncodingEnum(acceptEncodingEnum);
			response.body(fis);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}


	private static boolean checkReferer(final ZRequest request) {
		if (!STU.hasContent(SERVER_CONFIGURATION.getStaticPath())
				&& CU.isNotEmpty(SERVER_CONFIGURATION.getStaticControllerReferersAllowed())) {

			final Set<String> staticControllerReferersAllowed = SERVER_CONFIGURATION.getStaticControllerReferersAllowed();

			final String referer = request.getHeader(HeaderEnum.REFERER.getName());
			if (!STU.hasContent(referer)) {
				return false;
			}

			final Optional<String> findAny = staticControllerReferersAllowed.stream().filter(v -> referer.contains(v))
					.findAny();
			if (!findAny.isPresent()) {
				return false;
			}
		}

		return true;
	}

	private static void httpStatus403(final ZResponse response) {
		response
		.contentType(ContentTypeEnum.APPLICATION_JSON.getTypeBytes())
		.httpStatus(HttpStatusEnum.HTTP_403.getStatus())
		.body(J.toJSONString(CR.error(HttpStatusEnum.HTTP_403.getMessage())));
	}

}
