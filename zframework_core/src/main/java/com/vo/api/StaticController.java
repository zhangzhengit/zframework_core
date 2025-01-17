package com.vo.api;

import com.vo.anno.ZController;
import com.vo.cache.ZMC;
import com.vo.core.CacheControlEnum;
import com.vo.core.ContentTypeEnum;
import com.vo.core.ZRequest;
import com.vo.core.ZResponse;
import com.vo.html.ResourcesLoader;
import com.vo.http.HttpStatusEnum;
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

	/**
	 * 100MB
	 */
	private static final int CAPACITY = 1024 * 1024 * 100;

	private final ZMC zmc = new ZMC(CAPACITY);

	@ZRequestMapping(mapping = { "/favicon\\.ico",
			"/.+\\.png$",
			"/.+\\.js$", "/.+\\.jpg$", "/.+\\.mp3$", "/.+\\.mp4$", "/.+\\.pdf$",
			"/.+\\.gif$", "/.+\\.doc$" ,"/.+\\.css$","/.+\\.html$"},
			isRegex = { true, true, true, true, true, true, true, true, true, true , true }, qps = 10000 * 10)

	@ZQPSLimitation(qps = 2000, type = ZQPSLimitationEnum.ZSESSIONID)
	@ZETag
	@ZCacheControl(value = { CacheControlEnum.PRIVATE, CacheControlEnum.MUST_REVALIDATE }, maxAge = 60 * 10)
	public void staticResources(final ZResponse response, final ZRequest request) {

		final String resourceName = request.getRequestURI();

		final int i = resourceName.indexOf(".");
		if (i <= -1) {
			response.httpStatus(HttpStatusEnum.HTTP_500.getCode()).body(CR.error("不支持无后缀的文件"));
			return;
		}

		final ContentTypeEnum cte = ContentTypeEnum.gType(resourceName.substring(i + 1));
		if (cte == null) {
			response.httpStatus(HttpStatusEnum.HTTP_500.getCode()).body(CR.error("不支持的文件类型"));
			return;
		}

		response.contentType(cte.getType());

		final String key = "staticResources" + '-' + resourceName;

		// FIXME 2025年1月17日 下午5:38:27 zhangzhen : 可以判断一下，如果请求的文件时.html/.css等可被压缩的，
		// 可以先压缩再add，get后再解压缩
		final byte[] ba = this.zmc.computeIfAbsent(key,
				() -> ResourcesLoader.loadStaticResourceAsByteArray(resourceName));

		response.body(ba);
	}

}
