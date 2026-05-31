package vo.zframework.email;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vo.zframework.exception.ValidatedException;
import vo.zframework.http.HttpStatusEnum;
import vo.zframework.validator.ZCustomValidator;

/**
 *	IP地址校验
 *
 * @author zhangzhen
 * @date 2025年1月29日 下午3:29:48
 *
 */
public class ZIpValidator implements ZCustomValidator {

	private final static String IP_REGEX = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
	private final static Pattern PATTERN = Pattern.compile(IP_REGEX);

	@Override
	public void validated(final Object object, final Field field) throws Exception {

		field.setAccessible(true);
		final Object v = field.get(object);

		final Matcher matcher = PATTERN.matcher(String.valueOf(v));
		if (!matcher.matches()) {
			final String message = object.getClass().getSimpleName() + "." + field.getName() + "值[" + v + "]不符合IP地址规则";
			throw new ValidatedException(message, HttpStatusEnum.HTTP_400.getStatus());
		}

	}

}
