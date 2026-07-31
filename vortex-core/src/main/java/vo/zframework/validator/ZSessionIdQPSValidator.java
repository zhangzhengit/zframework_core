package vo.zframework.validator;

import java.lang.reflect.Field;

import vo.zframework.anno.ZCustom;
import vo.zframework.anno.ZValue;
import vo.zframework.common.RU;
import vo.zframework.exception.ValidatedException;

/**
 * 对同一个 ZSESSIONID 的限制
 *
 * @author zhangzhen
 * @date 2023年12月11日
 *
 */
public class ZSessionIdQPSValidator implements ZCustomValidator{

	public static final int MIN_VALUE = 10;

	public static final int MAX_VALUE = 10000 * 1000;

	public static final int DEFAULT_VALUE = 10000 * 200;

	@Override
	public void validated(final Object object, final Field field) throws Exception {

		final Integer v = (Integer) RU.getFiledValue(object, field);

		if ((v % MIN_VALUE) != 0) {
			final String message = field.getAnnotation(ZCustom.class).message();

			final String pName = field.isAnnotationPresent(ZValue.class)
					? "[" + field.getAnnotation(ZValue.class).name() + "]"
							: "";
			final String t = object.getClass().getSimpleName() + "." + field.getName() + " " + pName + " 必须配置为可以被 "
					+ MIN_VALUE + " (" + ZSessionIdQPSValidator.class.getCanonicalName() + ".MIN_VALUE)"
					+ " 整除";

			final String format = String.format(message, t);
			throw new ValidatedException(format);
		}
	}

}
