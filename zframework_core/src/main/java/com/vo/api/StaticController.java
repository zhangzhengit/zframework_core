package com.vo.api;

import java.io.InputStream;

import com.vo.anno.ZController;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.core.ContentTypeEnum;
import com.vo.core.HeaderEnum;
import com.vo.core.ZGzip;
import com.vo.core.ZMappingRegex;
import com.vo.core.ZRequest;
import com.vo.core.ZResponse;
import com.vo.core.ZSingleton;
import com.vo.html.ResourcesLoader;
import com.vo.http.HttpStatus;
import com.vo.http.ZETag;
import com.vo.http.ZQPSLimitation;
import com.vo.http.ZQPSLimitationEnum;
import com.vo.http.ZRequestMapping;
import com.votool.common.CR;

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

	@ZETag
	@ZRequestMapping(mapping = { "/favicon\\.ico",
			"/.+\\.png$",
			"/.+\\.js$", "/.+\\.jpg$", "/.+\\.mp3$", "/.+\\.mp4$", "/.+\\.pdf$",
			"/.+\\.gif$", "/.+\\.doc$" },
	isRegex = { true, true, true, true, true, true, true, true, true }, qps = 10000)
	public void staticResources(final ZResponse response,final ZRequest request) {

		final String resourceName = String.valueOf(ZMappingRegex.getAndRemove());

		final int i = resourceName.indexOf(".");
		if (i <= -1) {
			response.httpStatus(HttpStatus.HTTP_500.getCode()).body(CR.error("不支持无后缀的文件"));
			return;
		}

		final ContentTypeEnum cte = ContentTypeEnum.gType(resourceName.substring(i + 1));
		if (cte == null) {
			response.httpStatus(HttpStatus.HTTP_500.getCode()).body(CR.error("不支持的文件类型"));
			return;
		}

		final InputStream inputStream = ResourcesLoader.loadStaticResourceAsInputStream(resourceName);
		response.body(inputStream);
		response.contentType(cte.getType());
	}

	@ZETag
	@ZRequestMapping(mapping = { "/.+\\.css$" }, isRegex = { true }, qps = 10000)
	@ZQPSLimitation(qps = 500, type = ZQPSLimitationEnum.ZSESSIONID)
	public void css(final ZResponse response,final ZRequest request) {

		final String resourceName = String.valueOf(ZMappingRegex.getAndRemove());

		final int i = resourceName.indexOf(".");
		if (i <= -1) {
			response.httpStatus(HttpStatus.HTTP_500.getCode()).body(CR.error("不支持无后缀的文件"));
			return;
		}

		final ContentTypeEnum cte = ContentTypeEnum.gType(resourceName.substring(i + 1));
		if (cte == null) {
			response.httpStatus(HttpStatus.HTTP_500.getCode()).body(CR.error(HttpStatus.HTTP_500.getMessage()));
			return;
		}

		final byte[] ba = ResourcesLoader.loadStaticResourceAsByteArray(resourceName);
		response.contentType(cte.getType()).body(ba);

	}

	/**
	 * 通用的html匹配接口
	 *
	 * @param response
	 * @param request
	 *
	 */
	@ZRequestMapping(mapping = { "/.+\\.html$" }, isRegex = { true }, qps = 10000)
	@ZQPSLimitation(qps = 200, type = ZQPSLimitationEnum.ZSESSIONID)
	public void html(final ZResponse response,final ZRequest request) {

		final String resourceName = String.valueOf(ZMappingRegex.getAndRemove());

		final int i = resourceName.indexOf(".");
		if (i <= -1) {
			response.httpStatus(HttpStatus.HTTP_500.getCode()).body(CR.error("不支持无后缀的文件"));
			return;
		}

		final ContentTypeEnum cte = ContentTypeEnum.gType(resourceName.substring(i + 1));
		if (cte == null) {
			response.httpStatus(HttpStatus.HTTP_500.getCode()).body(CR.error(HttpStatus.HTTP_500.getMessage()));
			return;
		}

		final String html = ResourcesLoader.loadStaticResourceString(resourceName);

		//		final ServerConfigurationProperties serverConfiguration = ZSingleton.getSingletonByClass(ServerConfigurationProperties.class);
		//		final Boolean gzipEnable = serverConfiguration.getGzipEnable();
		//		final boolean gzipContains = serverConfiguration.gzipContains(ContentTypeEnum.TEXT_HTML.getType());
		//
		//		if (gzipEnable && gzipContains && request.isSupportGZIP()) {
		//			final byte[] ba = ZGzip.compress(html);
		//			response.contentType(cte.getType()).header(HeaderEnum.CONTENT_ENCODING.getName(), HeaderEnum.GZIP.getName()).body(ba);
		//		} else {
		response.contentType(cte.getType()).body(html);
		//		}
	}
}
