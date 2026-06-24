package vo.zframework.http.response;

import vo.zframework.common.CR;
import vo.zframework.common.J;
import vo.zframework.enums.ConnectionEnum;
import vo.zframework.enums.ContentTypeEnum;
import vo.zframework.enums.HeaderEnum;
import vo.zframework.enums.HttpStatusEnum;

/**
 * 一些响应
 *
 * @author zhangzhen
 * @date 2025年1月21日 下午4:24:43
 *
 */
public class ReU {

	public static ZResponse response429(final String message, final boolean keepAlive) {
		 return	new ZResponse()
			.contentType(ContentTypeEnum.APPLICATION_JSON.getTypeBytes())
			.header(HeaderEnum.CONNECTION.getNameBytes(),
					keepAlive ? ConnectionEnum.KEEP_ALIVE.getValueBytes() : ConnectionEnum.CLOSE.getValueBytes())
			.httpStatus(HttpStatusEnum.HTTP_429.getStatus())
			.body(J.toJSONString(CR.error(message)))
			;
	}

	public static ZResponse gResponse429(final String message, final boolean keepAlive) {
		return	new ZResponse()
		.contentType(ContentTypeEnum.APPLICATION_JSON.getTypeBytes())
		.header(HeaderEnum.CONNECTION.getNameBytes(),
				keepAlive ? ConnectionEnum.KEEP_ALIVE.getValueBytes() : ConnectionEnum.CLOSE.getValueBytes())
		.httpStatus(HttpStatusEnum.HTTP_429.getStatus())
		.body(J.toJSONString(CR.error(message)));

	}

	public static ZResponse response405(final String message, final boolean keepAlive) {
		final ZResponse r = new ZResponse()
				.httpStatus(HttpStatusEnum.HTTP_405.getStatus())
				.header(HeaderEnum.CONNECTION.getNameBytes(),
						keepAlive ? ConnectionEnum.KEEP_ALIVE.getValueBytes() : ConnectionEnum.CLOSE.getValueBytes())
				.contentType(ContentTypeEnum.APPLICATION_JSON.getTypeBytes())
				.body(J.toJSONString(CR.error("请求Method不支持：[" + message + "]")))
				;
		return r;
	}

	public static ZResponse response404(final String message, final boolean keepAlive) {
		final ZResponse r = new ZResponse()
				.httpStatus(HttpStatusEnum.HTTP_404.getStatus())
				.contentType(ContentTypeEnum.APPLICATION_JSON.getTypeBytes())
				.header(HeaderEnum.CONNECTION.getNameBytes(),
						keepAlive ? ConnectionEnum.KEEP_ALIVE.getValueBytes() : ConnectionEnum.CLOSE.getValueBytes())
				.body(J.toJSONString(CR.error("请求路径不存在[" + message + "]")));
		return r;
	}

	public static ZResponse response400(final String message, final boolean keepAlive) {
		final ZResponse r = new ZResponse()
				.header(HeaderEnum.CONNECTION.getNameBytes(),
						keepAlive ? ConnectionEnum.KEEP_ALIVE.getValueBytes() : ConnectionEnum.CLOSE.getValueBytes())
				.httpStatus(HttpStatusEnum.HTTP_400.getStatus())
				.contentType(ContentTypeEnum.APPLICATION_JSON.getTypeBytes())
				.body(J.toJSONString(CR.error(HttpStatusEnum.HTTP_400.getMessage() + "[" + message + "]")));
		return r;
	}

	public static ZResponse response431(final String message, final boolean keepAlive) {
		final ZResponse r = new ZResponse()
				.header(HeaderEnum.CONNECTION.getNameBytes(),
						keepAlive ? ConnectionEnum.KEEP_ALIVE.getValueBytes() : ConnectionEnum.CLOSE.getValueBytes())
				.httpStatus(HttpStatusEnum.HTTP_431.getStatus())
				.contentType(ContentTypeEnum.APPLICATION_JSON.getTypeBytes())
				.body(J.toJSONString(CR.error(HttpStatusEnum.HTTP_431.getMessage() + "[" + message + "]")));
		return r;
	}

}
