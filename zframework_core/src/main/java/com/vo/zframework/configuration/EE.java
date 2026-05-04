package com.vo.zframework.configuration;

import org.codehaus.janino.ExpressionEvaluator;

/**
 * 执行配置java语句表达式，如： 60 * 60 ，执行结果：3600
 *
 * @author zhangzhen
 * @date 2025年12月26日 18:03:08
 */
public class EE {
	
	public static Object execute(final String command) {
		try {
		    final ExpressionEvaluator ee = new ExpressionEvaluator();
		    ee.cook(command);
		    final Object result = ee.evaluate(null);
		    return result;
		} catch (final Exception e) {
		    e.printStackTrace();
		}
		
		return command;
	}

}
