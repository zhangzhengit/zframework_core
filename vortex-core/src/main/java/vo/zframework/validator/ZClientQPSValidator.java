package vo.zframework.validator;

import java.lang.reflect.Field;

import vo.zframework.anno.ZCustom;
import vo.zframework.anno.ZValue;
import vo.zframework.common.RU;
import vo.zframework.enums.QPSEnum;
import vo.zframework.exception.ValidatedException;

/**
 * server.client.qps 配置项的值必须可以被 MIN_VALUE 整除
 *
 * @author zhangzhen
 * @date 2023年11月24日
 *
 */
public class ZClientQPSValidator implements ZCustomValidator {

	/**
	 * 针对于同一个客户端的QPS限制最小值
	 */
	public static final int MIN_VALUE = 10;
	public static final int MAX_VALUE = 10000 * 1000;

	public static final int DEFAULT_VALUE = 10000 * 200;

	@Override
	public void validated(final Object object, final Field field) throws Exception {

		final Integer v = (Integer) RU.getFiledValue(object, field);

		if ((v % QPSEnum.CLIENT.getMinValue()) != 0) {
			final String message = field.getAnnotation(ZCustom.class).message();

			final String pName = field.isAnnotationPresent(ZValue.class)
					? "[" + field.getAnnotation(ZValue.class).name() + "]"
							: "";

			final String t = object.getClass().getSimpleName() + "." + field.getName() + " " + pName + " 必须配置为可以被 "
					+ QPSEnum.CLIENT.getMinValue() + " 整除";

			final String format = String.format(message, t);

			throw new ValidatedException(format);
		}
	}

}
