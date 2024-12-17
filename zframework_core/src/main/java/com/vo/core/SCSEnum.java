package com.vo.core;

/**
 * SocketChannel当前读取http请求报文的状态
 *
 * @author zhangzhen
 * @date 2024年12月17日 上午11:32:40
 *
 */
public enum SCSEnum {

	/**
	 * 开始读取http请求
	 */
	HTTP_START,

	/**
	 * 开始读取header
	 */
	HEADER_START,

	/**
	 * 读完了header
	 */
	HEADER_END,

	/**
	 * 开始读取body
	 */
	BODY_START,

	/**
	 * 读完了body
	 */
	BODY_END,

	/**
	 * 读完了一个http请求
	 */
	HTTP_END;

	/**
	 * 返回当前读取状态的下一个状态，按照上面的枚举值的上下顺序而来
	 *
	 * @param scsEnum
	 * @return
	 */
	public static SCSEnum nextStatus(final SCSEnum scsEnum) {
		switch (scsEnum) {
		case HTTP_START:
			return HEADER_START;

		case HEADER_START:
			return SCSEnum.HEADER_END;

		case HEADER_END:
			return SCSEnum.BODY_START;

		case BODY_START:
			return SCSEnum.BODY_END;

		case BODY_END:
			return SCSEnum.HTTP_END;

		case HTTP_END:
			return SCSEnum.HTTP_START;

		default:
			break;
		}

		return null;
	}
}
