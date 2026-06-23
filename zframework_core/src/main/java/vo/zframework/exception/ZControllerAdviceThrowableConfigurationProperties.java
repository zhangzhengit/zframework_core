package vo.zframework.exception;

import vo.zframework.anno.ZNotNull;
import vo.zframework.configuration.properties.ZConfigurationProperties;

/**
 * 内置的 ZControllerAdviceThrowable 类的 错误码
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@ZConfigurationProperties(prefix = "controller.advice")
public class ZControllerAdviceThrowableConfigurationProperties {

	@ZNotNull
	private int errorCode = 50000;

	public int getErrorCode() {
		return this.errorCode;
	}

	public void setErrorCode(final Integer errorCode) {
		this.errorCode = errorCode;
	}

}
