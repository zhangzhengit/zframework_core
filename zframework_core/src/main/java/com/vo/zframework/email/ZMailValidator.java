package com.vo.zframework.email;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vo.zframework.exception.ValidatedException;
import com.vo.zframework.http.HttpStatusEnum;
import com.vo.zframework.validator.ZCustomValidator;

/**
 * 邮件地址校验
 *
 * @author zhangzhen
 * @date 2025年1月18日 下午9:20:23
 *
 */
public class ZMailValidator implements ZCustomValidator {

	public static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

	@Override
	public void validated(final Object object, final Field field) throws Exception {

		field.setAccessible(true);

		final Object email = field.get(object);

		final Pattern pattern = Pattern.compile(EMAIL_REGEX);

		if (email instanceof String) {

			final Matcher matcher = pattern.matcher(String.valueOf(email));
			if (!matcher.matches()) {
				final String message = object.getClass().getSimpleName() + "." + field.getName() + "值[" + email
						+ "]不符合邮箱地址规则";
				throw new ValidatedException(message, HttpStatusEnum.HTTP_400.getCode());
			}
		} else if (email instanceof Set) {
			final Set set = (Set) email;
			for (final Object e : set) {
				final Matcher matcher = pattern.matcher(String.valueOf(e));
				if (!matcher.matches()) {
					final String message = object.getClass().getSimpleName() + "." + field.getName() + "值[" + e
							+ "]不符合邮箱地址规则";
					throw new ValidatedException(message, HttpStatusEnum.HTTP_400.getCode());
				}
			}
		}



	}

}
