package com.vo.exception;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.vo.anno.ZComponent;
import com.vo.core.Task;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;

import cn.hutool.core.collection.CollUtil;

/**
 * 运行时处理 @ZControllerAdvice 定义的方法
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@ZComponent
public class ZControllerAdviceActuator {

	private static final ZLog2 LOG = ZLog2.getInstance();

	/**
	 * 找一个异常处理器来处理API方法的异常，如果有自定义的匹配异常则使用此异常，否则使用内置的默认处理器
	 *
	 * @param throwable
	 * @return
	 *
	 */
	public Object execute(final Throwable throwable) {

		final String message = Task.gExceptionMessage(throwable);
		LOG.error("执行异常,message={}", message);

		final List<ZControllerAdviceBody> list = ZControllerAdviceScanner.LIST;
		if (CollUtil.isNotEmpty(list)) {
			for (final ZControllerAdviceBody zcadto : list) {
				if (zcadto.getThrowable().getCanonicalName().equals(throwable.getClass().getCanonicalName())) {

					try {
						return zcadto.getMethod().invoke(zcadto.getObject(), throwable);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						e.printStackTrace();
					}

				} else if (throwable.getCause() != null && zcadto.getThrowable().getCanonicalName()
						.equals(throwable.getCause().getClass().getCanonicalName())) {

					try {
						return zcadto.getMethod().invoke(zcadto.getObject(), throwable);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			}
		}

		// 自定义的异常处理器都不匹配，使用内置的默认处理器来处理
		return ZContext.getBean(ZControllerAdviceThrowable.class).throwZ(throwable);
	}

}
