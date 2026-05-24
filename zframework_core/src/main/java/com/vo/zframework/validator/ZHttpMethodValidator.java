package com.vo.zframework.validator;

import java.lang.reflect.Field;
import java.util.Arrays;

import com.vo.zframework.enums.MethodEnum;
import com.vo.zframework.exception.ValidatedException;

/**
 * 校验配置的http METHOD 是否合法
 *
 * @author zhangzhen
 * @date 2026年5月24日 05:20:08
 */
public class ZHttpMethodValidator implements ZCustomValidator {

	@Override
	public void validated(final Object object, final Field field) throws Exception {

		field.setAccessible(true);
		final Object value = field.get(object);
		final String ms = String.valueOf(value);


		final String[] a = ms.split(",");
		for (final String a1 : a) {
			final boolean methodStringUpper = MethodEnum.isMethodStringUpper(a1);
			if (!methodStringUpper) {
				final String t =
						"\r\n\t"
						+ object.getClass().getSimpleName() + "." + field.getName()
						+ ""
						+ "\r\n\t"
						+ "必须配置为 "
						+ Arrays.toString(MethodEnum.values())
						+ "中的值"
						+ "\r\n\t"
						+ "配置多个用,分隔."
						+ "\r\n\t"
						+ "当前配置值:" + ms
						+ "\r\n\t"
						;
				final String message = field.getAnnotation(ZCustom.class).message();

				final String format = String.format(message, t);
				throw new ValidatedException(format);

			}
		}













	}

}
