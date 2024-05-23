package com.vo.anno;

/**
 * 用于程序启动后，执行一些操作，在所有bean初始化以后http服务器启动之前执行。
 * 如定义多个，可以用 @ZOrder 标记执行顺序
 *
 * run(String... args) 接收命令行传来的参数
 * args 为下面形式传递的参数
 *
 * java -jar app.jar arg1 arg2 arg3 zhangsan lisi 123 ABC
 *
 * 则 args 为 [arg1, arg2, arg3, zhangsan, lisi, 123, ABC] 的String[]
 *
 * @author zhangzhen
 * @date 2023年11月15日
 *
 */
public interface ZCommandLineRunner {

	/**
	 * 执行自定义操作
	 *
	 * @param args
	 * @throws Exception
	 *
	 */
	void run(String... args) throws Exception;

}
