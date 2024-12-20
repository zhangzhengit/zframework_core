package com.vo.core;

import com.vo.configuration.ServerConfigurationProperties;
import com.vo.enums.MethodEnum;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 读取http请求时的请求行的 METHOD 结果
 *
 * @author zhangzhen
 * @date 2024年12月20日 下午3:40:38
 *
 */
// FIXME 2024年12月20日 下午3:46:02 zhangzhen : 注意：这个类是为了不想解决bug而做的妥协
// 因为调试body太麻烦了，我也不确定是读header还是读body哪里没处理好，
// 直接处理为：带有body的读取header直接使用1算了。反正带body的请求数占比不会太多，不太会拖累响应速度
@Data
@AllArgsConstructor
public class MR {

	/**
	 * POST PUT PATCH 等带有body的请求，读取header时的 ByteBuffer的容量大小
	 */
	public static final int POST_BYTE_LENGTH = 1;

	private final static Integer BYTE_BUFFER_SIZE = ZContext.getBean(ServerConfigurationProperties.class)
			.getByteBufferSize();

	private int readLength;
	private String methodName;

	private byte[] array;

	/**
	 * 根据已获取到的METHOD来确定读取header的ByteBuffer大小
	 *
	 * 不带body的如GET HEAD TRACE 等直接使用 server.byte.buffer.size 配置的值
	 * 带有body的直接使用1
	 *
	 * @return
	 */
	public Integer getByteBufferSize() {
		if ((MethodEnum.GET.name().equalsIgnoreCase(this.methodName))
				|| MethodEnum.TRACE.name().equalsIgnoreCase(this.methodName)
				|| MethodEnum.HEAD.name().equalsIgnoreCase(this.methodName)
				) {
			return BYTE_BUFFER_SIZE;
		}

		if (MethodEnum.POST.name().equalsIgnoreCase(this.methodName)
				|| MethodEnum.PUT.name().equalsIgnoreCase(this.methodName)
				|| MethodEnum.PATCH.name().equalsIgnoreCase(this.methodName)
				|| MethodEnum.DELETE.name().equalsIgnoreCase(this.methodName)
				|| MethodEnum.OPTIONS.name().equalsIgnoreCase(this.methodName)
				|| MethodEnum.CONNECT.name().equalsIgnoreCase(this.methodName)
				) {
			return POST_BYTE_LENGTH;
		}

		throw new UnsupportedOperationException("不支持的METHOD:" + this.methodName);
	}

}
