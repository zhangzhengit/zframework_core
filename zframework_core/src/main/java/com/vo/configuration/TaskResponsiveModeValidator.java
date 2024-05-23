package com.vo.configuration;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

import com.vo.anno.ZValue;
import com.vo.exception.ValidatedException;
import com.vo.validator.ZCustom;
import com.vo.validator.ZCustomValidator;

/**
 * 校验自定义的请求的响应模式
 *
 * @author zhangzhen
 * @date 2024年2月10日
 *
 */
public class TaskResponsiveModeValidator implements ZCustomValidator {

	@Override
	public void validated(final Object object, final Field field) throws Exception {
		field.setAccessible(true);
		final String v = (String) field.get(object);

		final Optional<TaskResponsiveModeEnum> findAny =
				Arrays.stream(TaskResponsiveModeEnum.values())
				.filter(e -> e.name().equals(v)).findAny();

		if (!findAny.isPresent()) {
			final String message = field.getAnnotation(ZCustom.class).message();

			final String pName = field.isAnnotationPresent(ZValue.class)
					? "[" + field.getAnnotation(ZValue.class).name() + "]"
					: "";
			final String t = object.getClass().getSimpleName() + "." + field.getName() + " " + pName
					+ " 声明["+
					 v +"]不支持，支持选项@see " + TaskResponsiveModeEnum.class.getCanonicalName();

			final String format = String.format(message, t);
			throw new ValidatedException(format);
		}

	}

}
