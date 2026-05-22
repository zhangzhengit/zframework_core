package com.vo.zframework.configuration;

import org.codehaus.janino.ExpressionEvaluator;

import com.vo.log.core.ZLog2;

/**
 * 执行配置java语句表达式，如： 60 * 60 ，执行结果：3600
 *
 * @author zhangzhen
 * @date 2025年12月26日 18:03:08
 */
public class EE {

//	static ZLog2 LOG = ZLog2.getInstance();

	public static Object execute(final String command) {
		try {
		    final ExpressionEvaluator ee = new ExpressionEvaluator();
		    ee.cook(command);
		    final Object result = ee.evaluate(null);
		    return result;
		} catch (final Exception e) {
		    e.printStackTrace();
		    // FIXME 2026年5月22日 13:20:29 zhangzhen : 考虑好，如果语句错误了怎么办，如["A" + d"]d前面少一个"符号编译报错
		    // 是放任不管只e.printST一下？
//		    LOG.error("表达式错误,请检查.expression={}", command);
		}
		return null;

	}

}
