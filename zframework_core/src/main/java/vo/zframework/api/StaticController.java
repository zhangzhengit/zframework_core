package vo.zframework.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import vo.zframework.anno.ZCacheControl;
import vo.zframework.anno.ZController;
import vo.zframework.anno.ZETag;
import vo.zframework.anno.ZQPSLimitation;
import vo.zframework.anno.ZRequestMapping;
import vo.zframework.common.CR;
import vo.zframework.common.CU;
import vo.zframework.common.J;
import vo.zframework.common.STU;
import vo.zframework.configuration.properties.ServerConfigurationProperties;
import vo.zframework.core.ZContext;
import vo.zframework.enums.AcceptEncodingEnum;
import vo.zframework.enums.CacheControlEnum;
import vo.zframework.enums.ContentTypeEnum;
import vo.zframework.enums.HeaderEnum;
import vo.zframework.enums.HttpStatusEnum;
import vo.zframework.enums.ZQPSLimitationEnum;
import vo.zframework.html.FIS;
import vo.zframework.html.ResourcesLoader;
import vo.zframework.http.request.ZRequest;
import vo.zframework.http.response.ZResponse;

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


	// FIXME 2026年6月25日 11:39:29 zhangzhen : -jar运行时resources下目录就用不到了br预压缩了，
	//　要不要提前把resources下的目录复制出来(和app.p一样)放在一个目录（用配置项），然后和server.static.path指定的
	// 目录一样，就可以用br预压缩了

	// FIXME 2026年6月24日 14:16:26 zhangzhen : 要不要加入"/.+\\..+$"支持A.B的形式来匹配所有的静态资源请求，并加一个配置项是否启用此mapping？
	@ZRequestMapping(mapping = { "/favicon\\.ico",
			"/.+\\.txt$",
			"/.+\\.png$",
			"/.+\\.wav$",
			"/.+\\.js$", "/.+\\.jpg$", "/.+\\.mp3$", "/.+\\.mp4$", "/.+\\.pdf$",
			"/.+\\.gif$", "/.+\\.doc$", "/.+\\.css$", "/.+\\.html$" }
		, isRegex = { true, true, true, true, true, true,
					true,true, true, true, true, true, true },
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

		final String sn = resourceName.substring(i + 1);
		final Map<String, String> ctm = SERVER_CONFIGURATION.getStaticControllerContentType();
		final String ct = ctm.get(sn);
		if (STU.isEmpty(ct)) {
			response.httpStatus(HttpStatusEnum.HTTP_500.getStatus())
					.contentType(ContentTypeEnum.APPLICATION_JSON.getTypeBytes())
					.body(J.toJSONString(CR.error("不支持的文件类型")));
			return;
		}

		response.contentType(ct);

		final FIS fis = ResourcesLoader.loadStaticResourceAsInputStream(resourceName);
		responseBody(response, request, fis);
	}

	private static void responseBody(final ZResponse response, final ZRequest request, final FIS fis) {
		if (fis.getFile() != null) {
			if (request.isSupportBR()) {
				// 支持br，并且是响应文件且已存在压缩好的.br文件，直接响应.br文件
				final File brFile = new File(fis.getFile() + StaticResourcespreCompressionService.BR);
				if (brFile.exists()) {
					try (final InputStream brInputStream = Files.newInputStream(Paths.get(brFile.getAbsolutePath()))) {
						final FIS brFis = new FIS(brInputStream, brFile);
						// 设置AcceptEncoding: br
						brFis.setAcceptEncodingEnum(AcceptEncodingEnum.BR);
						response.body(brFis);
						return;
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
			}

			if (request.isSupportZSTD()) {
				// 支持zstd，并且是响应文件且已存在压缩好的.zstd文件，直接响应.zstd文件
				final File zstdFile = new File(fis.getFile() + StaticResourcespreCompressionService.ZSTD);
				if (zstdFile.exists()) {
					try (final InputStream zstdInputStream = Files.newInputStream(Paths.get(zstdFile.getAbsolutePath()))) {
						final FIS zstdFis = new FIS(zstdInputStream, zstdFile);
						// 设置AcceptEncoding: zstd
						zstdFis.setAcceptEncodingEnum(AcceptEncodingEnum.ZSTD);
						response.body(zstdFis);
						return;
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
			}

			if (request.isSupportGZIP()) {
				// 支持gzip，并且是响应文件且已存在压缩好的.gzip文件，直接响应.gzip文件
				final File gzipFile = new File(fis.getFile() + StaticResourcespreCompressionService.GZIP);
				if (gzipFile.exists()) {
					try (final InputStream brInputStream = Files
							.newInputStream(Paths.get(gzipFile.getAbsolutePath()))) {
						final FIS gzipFis = new FIS(brInputStream, gzipFile);
						// 设置AcceptEncoding: gzip
						gzipFis.setAcceptEncodingEnum(AcceptEncodingEnum.GZIP);
						response.body(gzipFis);
						return;
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		// 响应原始文件
		response.body(fis);
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
		.body(J.toJSONString(CR.error("无权访问")));
	}

}
