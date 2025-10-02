package com.vo.configuration;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZNotEmtpy;

/**
 * 管理后台的配置
 *
 * @author zhangzhen
 * @date 2023年7月12日
 *
 */
@ZConfigurationProperties(prefix = "admin")
public class AdminConfiguration {

	@ZNotEmtpy
	private String userName = "admin";

	@ZNotEmtpy
	private String password = "admin";

	public String getUserName() {
		return this.userName;
	}

	public void setUserName(final String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return this.password;
	} 

	public void setPassword(final String password) {
		this.password = password;
	}

}