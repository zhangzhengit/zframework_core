package com.vo.api;

import com.vo.anno.ZController;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.core.HeaderEnum;
import com.vo.core.ZGzip;
import com.vo.core.ZMappingRegex;
import com.vo.core.ZRequest;
import com.vo.core.ZResponse;
import com.vo.core.ZSingleton;
import com.vo.html.ResourcesLoader;
import com.vo.http.HttpStatus;
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

	public static final String CONTENT_ENCODING = "Content-Encoding";

	@ZRequestMapping(mapping = { "/favicon\\.ico", "/.+\\.js$", "/.+\\.jpg$", "/.+\\.mp3$", "/.+\\.mp4$", "/.+\\.pdf$",
			"/.+\\.gif$", "/.+\\.doc$" }, isRegex = { true, true, true, true, true, true, true, true }, qps = 10000)

	public void staticResources(final ZResponse response,final ZRequest request) {

		final String resourceName = String.valueOf(ZMappingRegex.getAndRemove());

		final int i = resourceName.indexOf(".");
		if (i <= -1) {
			response.httpStatus(HttpStatus.HTTP_500.getCode()).body(CR.error("不支持无后缀的文件"));
			return;
		}

		final HeaderEnum cte = HeaderEnum.gType(resourceName.substring(i + 1));
		if (cte == null) {
			response.httpStatus(HttpStatus.HTTP_500.getCode()).body(CR.error(HttpStatus.HTTP_500.getMessage()));
		}

		final byte[] loadByteArray = ResourcesLoader.loadStaticResourceByteArray(resourceName);
		// FIXME 2023年7月19日 下午8:20:39 zhanghen: TODO 改为和Socket 一样，一边读取一边写入到OutStream
		response.contentType(cte.getType()).body(loadByteArray);

//		ResourcesLoader.writeResourceToOutputStreamThenClose(resourceName, cte, response);
	}

	@ZRequestMapping(mapping = { "/.+\\.css$" }, isRegex = { true }, qps = 10000)
	public void css(final ZResponse response,final ZRequest request) {

		final String resourceName = String.valueOf(ZMappingRegex.getAndRemove());

		final int i = resourceName.indexOf(".");
		if (i <= -1) {

			response
				.httpStatus(HttpStatus.HTTP_500.getCode())
				.body(CR.error("不支持无后缀的文件"))
//				.write()
				;

			return;
		}

		final HeaderEnum cte = HeaderEnum.gType(resourceName.substring(i + 1));
		if (cte == null) {
			response.httpStatus(HttpStatus.HTTP_500.getCode()).body(CR.error(HttpStatus.HTTP_500.getMessage()));
			return;
		}

		final ServerConfigurationProperties serverConfiguration = ZSingleton.getSingletonByClass(ServerConfigurationProperties.class);

		final Boolean gzipEnable = serverConfiguration.getGzipEnable();
		final boolean gzipContains = serverConfiguration.gzipContains(HeaderEnum.CSS.getType());
		if (gzipEnable && gzipContains && request.isSupportGZIP()) {

			final String string = ResourcesLoader.loadStaticResourceString(resourceName);
			final byte[] ba = ZGzip.compress(string);

			response.contentType(cte.getType()).header(StaticController.CONTENT_ENCODING, ZRequest.GZIP).body(ba);

		} else {
			final byte[] ba = ResourcesLoader.loadStaticResourceByteArray(resourceName);

			response.contentType(cte.getType()).body(ba);
		}

	}

	/**
	 * 通用的html匹配接口
	 *
	 * @param response
	 * @param request
	 *
	 */
	@ZRequestMapping(mapping = { "/.+\\.html$" }, isRegex = { true }, qps = 10000)
	public void html(final ZResponse response,final ZRequest request) {

		final String resourceName = String.valueOf(ZMappingRegex.getAndRemove());

		final int i = resourceName.indexOf(".");
		if (i <= -1) {
			response.httpStatus(HttpStatus.HTTP_500.getCode()).body(CR.error("不支持无后缀的文件"));
			return;
		}

		final HeaderEnum cte = HeaderEnum.gType(resourceName.substring(i + 1));
		if (cte == null) {
			response.httpStatus(HttpStatus.HTTP_500.getCode()).body(CR.error(HttpStatus.HTTP_500.getMessage()));
			return;
		}

		final ServerConfigurationProperties serverConfiguration = ZSingleton.getSingletonByClass(ServerConfigurationProperties.class);
		final Boolean gzipEnable = serverConfiguration.getGzipEnable();
		final boolean gzipContains = serverConfiguration.gzipContains(HeaderEnum.HTML.getType());
		final String html = ResourcesLoader.loadStaticResourceString(resourceName);

		if (gzipEnable && gzipContains && request.isSupportGZIP()) {
			final byte[] ba = ZGzip.compress(html);
			response.contentType(cte.getType()).header(StaticController.CONTENT_ENCODING, ZRequest.GZIP).body(ba);
		} else {
			response.contentType(cte.getType()).body(html);
		}
	}
}
