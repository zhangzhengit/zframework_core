package vo.vortex.api;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import vo.vortex.anno.ZController;
import vo.vortex.cache.CU;
import vo.vortex.cache.J;
import vo.vortex.cache.STU;
import vo.vortex.common.CR;
import vo.vortex.configuration.ServerConfigurationProperties;
import vo.vortex.core.CacheControlEnum;
import vo.vortex.core.ContentTypeEnum;
import vo.vortex.core.HeaderEnum;
import vo.vortex.core.ZContext;
import vo.vortex.core.ZRequest;
import vo.vortex.core.ZResponse;
import vo.vortex.html.ResourcesLoader;
import vo.vortex.http.HttpStatusEnum;
import vo.vortex.http.ZCacheControl;
import vo.vortex.http.ZETag;
import vo.vortex.http.ZQPSLimitation;
import vo.vortex.http.ZQPSLimitationEnum;
import vo.vortex.http.ZRequestMapping;

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

	@ZRequestMapping(mapping = { "/favicon\\.ico",
			"/.+\\.txt$",
			"/.+\\.png$",
			"/.+\\.wav$",
			"/.+\\.js$", "/.+\\.jpg$", "/.+\\.mp3$", "/.+\\.mp4$", "/.+\\.pdf$",
			"/.+\\.gif$", "/.+\\.doc$", "/.+\\.css$", "/.+\\.html$" }
		, isRegex = { true, true, true, true, true, true,
					true,true, true, true, true, true, true },
				count = 10000 * 10)

	@ZQPSLimitation(count = 2000, type = ZQPSLimitationEnum.ZSESSIONID)
	@ZETag
	@ZCacheControl(value = { CacheControlEnum.PRIVATE, CacheControlEnum.MUST_REVALIDATE }, maxAge = 60 * 10)
	public void staticResources(final ZResponse response, final ZRequest request) {

		if (!checkReferer(request)) {
			httpStatus403(response);
			return;
		}

		final String resourceName = request.getRequestURI();

		final int i = resourceName.lastIndexOf(".");
		if (i <= -1) {
			response.httpStatus(HttpStatusEnum.HTTP_500.getCode())
					.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
					.body(J.toJSONString(CR.error("不支持无后缀的文件")));
			return;
		}

		final String sn = resourceName.substring(i + 1);
		final Map<String, String> ctm = SERVER_CONFIGURATION.getStaticControllerContentType();
		final String ct = ctm.get(sn);
		if (STU.isEmpty(ct)) {
			response.httpStatus(HttpStatusEnum.HTTP_500.getCode())
					.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
					.body(J.toJSONString(CR.error("不支持的文件类型")));
			return;
		}

		response.contentType(ct);

		final InputStream inputStream = ResourcesLoader.loadStaticResourceAsInputStream(resourceName);
		if (inputStream instanceof FileInputStream) {
			response.body((FileInputStream) inputStream);
		} else {
			response.body(inputStream);
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
		.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
		.httpStatus(HttpStatusEnum.HTTP_403.getCode())
		.body(J.toJSONString(CR.error("无权访问")));
	}

}
