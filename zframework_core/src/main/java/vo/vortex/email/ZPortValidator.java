package vo.vortex.email;

import java.lang.reflect.Field;

import vo.vortex.core.PortChecker;
import vo.vortex.exception.ValidatedException;
import vo.vortex.validator.ZCustomValidator;

/**
 * 端口号校验
 *
 * @author zhangzhen
 * @date 2025年1月18日 下午9:20:23
 *
 */
public class ZPortValidator implements ZCustomValidator {

	public static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

	@Override
	public void validated(final Object object, final Field field) throws Exception {

		field.setAccessible(true);

		final Object value = field.get(object);

		int port = 0;
		try {
			port = Integer.parseInt(String.valueOf(value));
		} catch (final Exception e) {
			final String message = object.getClass().getSimpleName() + "." + field.getName() + "值[" + value
					+ "]不符合端口号规则";
			throw new ValidatedException(message);
		}

		if (!PortChecker.isPortIllegal(port)) {
			final String message = object.getClass().getSimpleName() + "." + field.getName() + "值[" + value
					+ "]不符合端口号规则";
			throw new ValidatedException(message);
		}

	}

}
