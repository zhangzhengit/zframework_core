//package vo.zframework.configuration.properties;
//
//import vo.zframework.anno.ZNotEmtpy;
//import vo.zframework.anno.ZNotNull;
//
///**
// * redis配置项
// *
// * @author zhangzhen
// * @date 2023年11月5日
// *
// */
//@ZConfigurationProperties(prefix = "redis")
//public class ZRedisConfigurationProperties {
//
//	@ZNotEmtpy
//	private String host = "localhost";
//
//	@ZNotNull
//	private int port = 6379;
//
//	@ZNotNull
//	private int timeout = 2000;
//
//	@ZNotNull
//	private int maxTotal = 20;
//
//	@ZNotNull
//	private int maxIdle = 2;
//
//	/**
//	 * 密码，可以为空，不做限制，也无默认值
//	 */
//	private String password;
//
//	public ZRedisConfigurationProperties(final String host, final int port, final int timeout, final int maxTotal,
//			final int maxIdle, final String password) {
//		this.host = host;
//		this.port = port;
//		this.timeout = timeout;
//		this.maxTotal = maxTotal;
//		this.maxIdle = maxIdle;
//		this.password = password;
//	}
//
//	public ZRedisConfigurationProperties() {
//	}
//
//	public String getHost() {
//		return this.host;
//	}
//
//	public void setHost(final String host) {
//		this.host = host;
//	}
//
//	public int getPort() {
//		return this.port;
//	}
//
//	public void setPort(final int port) {
//		this.port = port;
//	}
//
//	public int getTimeout() {
//		return this.timeout;
//	}
//
//	public void setTimeout(final int timeout) {
//		this.timeout = timeout;
//	}
//
//	public int getMaxTotal() {
//		return this.maxTotal;
//	}
//
//	public void setMaxTotal(final int maxTotal) {
//		this.maxTotal = maxTotal;
//	}
//
//	public int getMaxIdle() {
//		return this.maxIdle;
//	}
//
//	public void setMaxIdle(final int maxIdle) {
//		this.maxIdle = maxIdle;
//	}
//
//	public String getPassword() {
//		return this.password;
//	}
//
//	public void setPassword(final String password) {
//		this.password = password;
//	}
//}
