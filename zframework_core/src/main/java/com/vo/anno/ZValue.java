package com.vo.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * 用在字段上，表示此字段值从配置文件[zframework.properties]中取值。
 * 如下，表示从配置文件中取 a.b 的值赋给String b字段。
 *
 *  @ZValue(name = "a.b")
 * 	private String b;
 *
 * 注意：配置文件优先于代码。如果配置文件中存在a.b，则b的值为配置文件中的a.b。
 *      不存在a.b,则b值默认为代码中的初始值(null或者随便什么值)
 *
 *
 * @author zhangzhen
 * @date 2023年6月29日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface ZValue {

	/**
	 * 对应配置文件中的key，来获取此key对应的value
	 *
	 * @return
	 *
	 */
	String name();

	/**
	 * 是否监听配置文件变动来更新值，可选项，默认[false]不监听。
	 * 设为true，则此注解标记的字段值会随着配置文件中的K的变化而自动更新
	 *
	 * @return
	 *
	 */
	boolean listenForChanges() default false;

}
