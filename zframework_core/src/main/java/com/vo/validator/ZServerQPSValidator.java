package com.vo.validator;

import java.lang.reflect.Field;

import com.vo.anno.ZValue;
import com.vo.core.QPSCounter;
import com.vo.core.QPSEnum;
import com.vo.exception.ValidatedException;

/**
 * 必须可以被 MIN_VALUE 整除
 *
 * @author zhangzhen
 * @date 2023年11月14日
 *
 */
public class ZServerQPSValidator implements ZCustomValidator {

	public static final int MIN_VALUE = 100;
	public static final int MAX_VALUE = 10000 * 100;
	public static final int DEFAULT_VALUE = 10000 * 20;

	@Override
	public void validated(final Object object, final Field field) throws Exception {

		try {
			field.setAccessible(true);
			final Object value = field.get(object);
			final Integer v = (Integer) value;

			if (v % QPSEnum.SERVER.getMinValue() != 0) {
				final String message = field.getAnnotation(ZCustom.class).message();

				final String pName = field.isAnnotationPresent(ZValue.class)
						? "[" + field.getAnnotation(ZValue.class).name() + "]"
						: "";
				final String t = object.getClass().getSimpleName() + "." + field.getName() + " " + pName + " 必须配置为可以被 "
						+ QPSEnum.SERVER.getMinValue() + " (" + QPSCounter.class.getCanonicalName() + ".QPS_MIN)"
						+ " 整除";

				final String format = String.format(message, t);
				throw new ValidatedException(format);

			}

		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw e;
		}

	}

}
