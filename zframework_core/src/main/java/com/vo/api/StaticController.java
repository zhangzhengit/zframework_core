package com.vo.api;

import java.io.InputStream;

import com.vo.anno.ZController;
import com.vo.core.CacheControlEnum;
import com.vo.core.ContentTypeEnum;
import com.vo.core.ZRequest;
import com.vo.core.ZResponse;
import com.vo.html.ResourcesLoader;
import com.vo.http.HttpStatus;
import com.vo.http.ZCacheControl;
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

	@ZRequestMapping(mapping = { "/favicon\\.ico",
			"/.+\\.png$",
			"/.+\\.js$", "/.+\\.jpg$", "/.+\\.mp3$", "/.+\\.mp4$", "/.+\\.pdf$",
			"/.+\\.gif$", "/.+\\.doc$" ,"/.+\\.css$","/.+\\.html$"},
			isRegex = { true, true, true, true, true, true, true, true, true, true , true }, qps = 10000 * 5)

	@ZQPSLimitation(qps = 100, type = ZQPSLimitationEnum.ZSESSIONID)
	@ZETag
	@ZCacheControl(value = { CacheControlEnum.PRIVATE, CacheControlEnum.MUST_REVALIDATE }, maxAge = 60 * 10)
	public void staticResources(final ZResponse response, final ZRequest request) {

		final String resourceName = request.getRequestURI();

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

		response.contentType(cte.getType());

		final InputStream inputStream = ResourcesLoader.loadStaticResourceAsInputStream(resourceName);
		response.body(inputStream);
	}

}
