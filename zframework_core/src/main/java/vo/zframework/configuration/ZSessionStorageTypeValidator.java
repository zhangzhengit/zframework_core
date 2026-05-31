package vo.zframework.configuration;

import java.lang.reflect.Field;
import java.util.Arrays;

import vo.zframework.enums.ZSessionStorageTypeEnum;
import vo.zframework.exception.ValidatedException;
import vo.zframework.validator.ZCustom;
import vo.zframework.validator.ZCustomValidator;

/**
 * 验证配置的 sessionStorageType
 *
 * @author zhangzhen
 * @date 2023年11月16日
 *
 */
public class ZSessionStorageTypeValidator implements ZCustomValidator {

	@Override
	public void validated(final Object object, final Field field) throws Exception {

		field.setAccessible(true);

		final String s = (String) field.get(object);

		if (ZSessionStorageTypeEnum.MEMORY.name().equals(s)) {

		} else if (ZSessionStorageTypeEnum.REDIS.name().equals(s)) {

		} else {
			final String message = field.getAnnotation(ZCustom.class).message();

			final String t = String.format(message,
					"SessionStorageType[" + s + "]不支持,支持类型为 " + Arrays.toString(ZSessionStorageTypeEnum.values()));

			throw new ValidatedException(t);
		}

	}

}
