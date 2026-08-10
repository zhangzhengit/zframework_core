package vo.vortex.configuration.properties;

import vo.vortex.anno.ZNotEmtpy;

/**
 * common 配置
 *
 * @author zhangzhen
 * @date 2024年2月17日
 *
 */
@ZConfigurationProperties(prefix = "common")
public class CommonConfigurationProperties {

	/**
	 * 指定的启动类名称,/resources/META-INF/下指定的启动配置文件的名称
	 */
	@ZNotEmtpy
	private String starterName = "vortex.factories";

	public String getStarterName() {
		return this.starterName;
	}

	public void setStarterName(final String starterName) {
		this.starterName = starterName;
	}

}
