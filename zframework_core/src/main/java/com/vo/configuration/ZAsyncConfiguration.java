package com.vo.configuration;

import com.vo.anno.ZAutowired;
import com.vo.anno.ZBean;
import com.vo.anno.ZConfiguration;
import com.votool.ze.ThreadModeEnum;
import com.votool.ze.ZE;
import com.votool.ze.ZES;

/**
 *
 * @ZAsync 配置类
 *
 * @author zhangzhen
 * @date 2023年7月8日
 *
 */
@ZConfiguration
public class ZAsyncConfiguration {

	private static final String ASYNC_GROUP = "async-Group";

	@ZAutowired
	private ZAsyncProperties zAsyncProperties;

	@ZBean
	public ZE zeZAsync() {
		return ZES.newZE(this.zAsyncProperties.getThreadCount(), ASYNC_GROUP,
				this.zAsyncProperties.getThreadNamePrefix(), ThreadModeEnum.LAZY);
	}

}
