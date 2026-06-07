package vo.zframework.core;

/**
 * 读取解析http请求的状态
 *
 * @author zhangzhen
 * @date 2026年5月24日 10:19:45
 */
public enum HttpParseStatusEnum {

	EXCEPTION,
	START,

	PARSE_REQUEST_LINE,

	CHECK_METHOD,

	CHECK_URI,

	CHECK_VERSION,

	PARSE_HEADER,

	PARSE_CONTENT_LENGTH,

	PARSE_BODY,

	PARSE_END,

	;


}
