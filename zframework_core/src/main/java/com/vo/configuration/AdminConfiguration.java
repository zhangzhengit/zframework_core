package com.vo.configuration;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZMax;
import com.vo.validator.ZMin;
import com.vo.validator.ZNotEmtpy;
import com.vo.validator.ZNotNull;
import com.vo.validator.ZStartWith;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理后台的配置
 *
 * @author zhangzhen
 * @date 2023年7月12日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZConfigurationProperties(prefix = "admin")
public class AdminConfiguration {

	@ZNotEmtpy
	private String userName = "admin";

	@ZNotEmtpy
	private String password = "admin";

}