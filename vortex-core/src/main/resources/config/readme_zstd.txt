
注意：
本工程没有使用zstd-jni的依赖，因为它打出的jar中占了6MB多，在一个空项目中所有的jar才占14MB
默认使用java原生的zstd。
如果有以下情况:

1、响应体大于1KB(默认的body压缩阈值)
2、有静态文件接口并且是可压缩的文件类型且配置了压缩这些后缀
3、追求极致的压缩速度


强烈推荐在工程中加入一下依赖来使用jni的zstd压缩
<dependency>
	<groupId>vo</groupId>
	<artifactId>vortext-starter-zstd</artifactId>
	<version>1.0.0-SNAPSHOT-jdk21</version>
</dependency>