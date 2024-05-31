package com.vo.cache;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZNotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 *
 * @author zhangzhen
 * @date 2024年5月31日 下午7:49:37
 * 
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZConfigurationProperties(prefix = "admin")
public class ZAdminConfigurationProperties {

	/**
	 * admin后台页面功能是否开启
	 */
	@ZNotNull
	private Boolean enable;

}
