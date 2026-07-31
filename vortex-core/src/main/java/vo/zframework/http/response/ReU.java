package vo.zframework.http.response;

import java.nio.charset.StandardCharsets;

import vo.zframework.common.CR;
import vo.zframework.common.J;
import vo.zframework.enums.AccessDeniedCodeEnum;
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

	private static final byte[] SERVICE_UNAVAILABLE_RESPONSE =
								("HTTP/1.1 " + HttpStatusEnum.HTTP_503.getStatus() + " " + HttpStatusEnum.HTTP_503.getMessage() + "\r\n" +
							    "Content-Length: 0\r\n" +
							    "Connection: close\r\n" +
							    "\r\n")
							    .getBytes(StandardCharsets.ISO_8859_1);

	public static byte[] g503Bytes() {
		return SERVICE_UNAVAILABLE_RESPONSE;
	}

	public static ZResponse response429(final String message, final boolean keepAlive) {
		return jsonResponse(keepAlive)
				.httpStatus(HttpStatusEnum.HTTP_429.getStatus())
				.body(J.toJSONString(CR.error(HttpStatusEnum.HTTP_429.getMessage() + " " + message)));
	}

	public static ZResponse response429API() {
		return new ZResponse()
						.contentType(ContentTypeEnum.APPLICATION_JSON.getTypeBytes())
						.httpStatus(HttpStatusEnum.HTTP_429.getStatus())
						.header(HeaderEnum.CONNECTION.getNameBytes(), ConnectionEnum.CLOSE.getValueBytes())
						.body(J.toJSONString(CR.error(AccessDeniedCodeEnum.API.getCode(),
								AccessDeniedCodeEnum.API.getInternalMessage())));
	}

	public static ZResponse response429ZSESSIONID() {
		return new ZResponse()
						.contentType(ContentTypeEnum.APPLICATION_JSON.getTypeBytes())
						.httpStatus(HttpStatusEnum.HTTP_429.getStatus())
						.header(HeaderEnum.CONNECTION.getNameBytes(), ConnectionEnum.CLOSE.getValueBytes())
						.body(J.toJSONString(CR.error(AccessDeniedCodeEnum.ZSESSIONID.getCode(),
								AccessDeniedCodeEnum.ZSESSIONID.getMessageToClient())));
	}

	public static ZResponse response405(final String message, final boolean keepAlive) {
		final ZResponse r = jsonResponse(keepAlive)
				.httpStatus(HttpStatusEnum.HTTP_405.getStatus())
				.body(J.toJSONString(CR.error(HttpStatusEnum.HTTP_405.getMessage() + " " + message)))
				;
		return r;
	}

	public static ZResponse response404(final String message, final boolean keepAlive) {
		final ZResponse r = jsonResponse(keepAlive)
				.httpStatus(HttpStatusEnum.HTTP_404.getStatus())
				.body(J.toJSONString(CR.error(HttpStatusEnum.HTTP_404.getMessage() + " " + message)));
		return r;
	}

	public static ZResponse response400(final String message, final boolean keepAlive) {
		final ZResponse r = jsonResponse(keepAlive)
				.httpStatus(HttpStatusEnum.HTTP_400.getStatus())
				.body(J.toJSONString(CR.error(HttpStatusEnum.HTTP_400.getMessage() + " " + message)));
		return r;
	}

	public static ZResponse response431(final String message, final boolean keepAlive) {
		final ZResponse r = jsonResponse(keepAlive)
				.httpStatus(HttpStatusEnum.HTTP_431.getStatus())
				.body(J.toJSONString(CR.error(HttpStatusEnum.HTTP_431.getMessage() + " " + message)));
		return r;
	}

	private static ZResponse jsonResponse(final boolean keepAlive) {
		return new ZResponse()
				.contentType(ContentTypeEnum.APPLICATION_JSON.getTypeBytes())
				.header(HeaderEnum.CONNECTION.getNameBytes(),
						keepAlive
						? ConnectionEnum.KEEP_ALIVE.getValueBytes()
						: ConnectionEnum.CLOSE.getValueBytes());
	}

}
