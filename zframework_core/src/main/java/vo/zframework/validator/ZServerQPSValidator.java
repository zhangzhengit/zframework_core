package vo.zframework.validator;

import java.lang.reflect.Field;

import vo.zframework.anno.ZCustom;
import vo.zframework.anno.ZValue;
import vo.zframework.common.RU;
import vo.zframework.enums.QPSEnum;
import vo.zframework.exception.ValidatedException;

/**
 * 必须可以被 MIN_VALUE 整除
 *
 * @author zhangzhen
 * @date 2023年11月14日
 *
 */
public class ZServerQPSValidator implements ZCustomValidator {

	/**
	 * 此值最小为1，即使[server.qps]支持了配置为0，此值也最小为1，因为会qps/此值
	 */
	public static final int MIN_VALUE = 1;
	public static final int MAX_VALUE = 10000 * 1000;
	public static final int DEFAULT_VALUE = 10000 * 200;

	@Override
	public void validated(final Object object, final Field field) throws Exception {

		final Integer v = (Integer) RU.getFiledValue(object, field);

		if ((v % QPSEnum.SERVER.getMinValue()) != 0) {
			final String message = field.getAnnotation(ZCustom.class).message();

			final String pName = field.isAnnotationPresent(ZValue.class)
					? "[" + field.getAnnotation(ZValue.class).name() + "]"
							: "";
			final String t = object.getClass().getSimpleName() + "." + field.getName() + " " + pName + " 必须配置为可以被 "
					+ QPSEnum.SERVER.getMinValue() + " (" + QPSEnum.class.getCanonicalName() + ".SERVER.getMinValue)"
					+ " 整除";

			final String format = String.format(message, t);
			throw new ValidatedException(format);

		}
	}

}
