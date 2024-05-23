package com.vo.validator;

import com.vo.anno.ZConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @ZLength 的message 配置
 *
 * @author zhangzhen
 * @date 2023年10月31日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
//@ZConfigurationProperties(prefix = "validator.constraints.length")
public class ZLengthMessageConfigurationProperties {

//	@ZNotEmtpy
	// FIXME 2023年11月1日 下午6:51:35 zhanghen: 如果此值配置为 validator.constraints.length.message=validator.constraints.length.message
	// 是否提示一下不能这么配，还是如果这么配就忽略掉，默认使用此类中message字段的值？
	private String message = "[%s]长度必须在[%s]和[%s]之间,当前值[%s]";

}
