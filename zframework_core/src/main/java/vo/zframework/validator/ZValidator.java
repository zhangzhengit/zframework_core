package vo.zframework.validator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import vo.zframework.anno.ZCustom;
import vo.zframework.anno.ZEndsWith;
import vo.zframework.anno.ZLength;
import vo.zframework.anno.ZMax;
import vo.zframework.anno.ZMin;
import vo.zframework.anno.ZNotEmtpy;
import vo.zframework.anno.ZNotNull;
import vo.zframework.anno.ZPositive;
import vo.zframework.anno.ZStartWith;
import vo.zframework.anno.ZUnique;
import vo.zframework.anno.ZValue;
import vo.zframework.bean.ZSingleton;
import vo.zframework.common.RU;
import vo.zframework.common.STU;
import vo.zframework.configuration.properties.ZConfigurationProperties;
import vo.zframework.enums.HttpStatusEnum;
import vo.zframework.exception.TypeNotSupportedExcpetion;
import vo.zframework.exception.ValidatedException;
import vo.zframework.http.Task;
import vo.zframework.scanner.ClassMap;
import vo.zframework.scanner.ZConfigurationPropertiesScanner;

/**
 * 验证器
 *
 * @author zhangzhen
 * @date 2023年10月15日
 *
 */
public class ZValidator {


	private static Set<Class<? extends Annotation>> VA_SET = Set.of(
			ZNotNull.class, ZNotEmtpy.class, ZStartWith.class,
			ZEndsWith.class, ZLength.class, ZMin.class,
			ZUnique.class,
			ZMax.class, ZPositive.class);

	public static void validatedZNotNull(final Object object, final Field field) {
		final ZNotNull nn = field.getAnnotation(ZNotNull.class);
		if (nn == null) {
			return;
		}

		final Object v = ZValidator.getFieldValue(object, field);
		if (v != null) {
			return;
		}

		ZValidator.throwZNotNullException(object, field);
	}

	public static void validatedZPositive(final Parameter p, final Object value) {
		if (value == null) {
			ZValidator.throwZNotNullException(p.getName());
			return;
		}

		if (!ZValidator.isZMinZMaxSupported(p.getType())) {
			throw new ValidatedException("@" + ZPositive.class.getSimpleName()
					+ " 只能用于Byte,Short,Integer,Long,Float,Double,BigDecimal,BigInteger,AtomicLong,AtomicInteger类型,当前用于["
					+ p.getName() + "]",HttpStatusEnum.HTTP_400.getStatus());
		}

		final double doubleValue = ((Number) value).doubleValue();
		if (doubleValue <= 0D) {
			final String message = ZPositive.MESSAGE;
			final String t = p.getName();

			final String format = String.format(message, t, value);

			throw new ValidatedException(format);
		}

	}

	public static void validatedZPositive(final Object object, final Field field) {
		final ZPositive zp = field.getAnnotation(ZPositive.class);
		if (zp == null) {
			return;
		}

		final Object v = ZValidator.getFieldValue(object, field);
		if (v == null) {
			ZValidator.throwZNotNullException(object, field);
			return;
		}

		if (!ZValidator.isZMinZMaxSupported(v.getClass())) {
			throw new ValidatedException("@" + ZPositive.class.getSimpleName()
					+ " 只能用于Byte,Short,Integer,Long,Float,Double,BigDecimal,BigInteger,AtomicLong,AtomicInteger类型,当前用于字段["
					+ field.getName() + "]");
		}

		final double doubleValue = ((Number) v).doubleValue();
		if (doubleValue <= 0D) {
			final String message = ZPositive.MESSAGE;
			final String t = object.getClass().getSimpleName() + "." + field.getName();
			final String pName = field.isAnnotationPresent(ZValue.class)
					? "[" + field.getAnnotation(ZValue.class).name() + "]"
							: "";

			final String itemName = gItemName(object, field);

			final String format = String.format(message, t + pName, v)
					+ (STU.isEmpty(itemName) ? "" : ("\r\n\t" + "请配置[" + itemName + "]为大于0的值"));

			throw new ValidatedException(format, HttpStatusEnum.HTTP_400.getStatus());
		}

	}

	public static boolean isString(final Class<?> cls) {
		return cls == String.class;
	}

	public static boolean isZCustomSupported(final Class<?> cls) {
		// @ZCustom 支持任何类型
		// FIXME 2023年11月14日 下午10:22:03 zhanghen: TODO 要不要判断并提示不能使基本类型？
		return true;
	}

	public static boolean isZMinZMaxSupported(final Class<?> cls) {
		final String nnnn = cls.getName();
		return "byte".equals(nnnn)
			|| "short".equals(nnnn)
			|| "int".equals(nnnn)
			|| "long".equals(nnnn)
			|| "float".equals(nnnn)
			|| "double".equals(nnnn)
			|| "byte".equals(nnnn)

		|| (cls == Byte.class) || (cls == Short.class) || (cls == Integer.class) || (cls == Long.class)
				|| (cls == Float.class) || (cls == Double.class) || (cls == BigDecimal.class) || (cls == BigInteger.class)
				|| (cls == AtomicLong.class) || (cls == AtomicInteger.class);
	}


	public static void validatedZUnique(final Object object, final Field field) {

		final ZUnique zu = field.getAnnotation(ZUnique.class);
		if (zu == null) {
			return;
		}

		final Object v = ZValidator.getFieldValue(object, field);

		final boolean add = ZUniqueHelper.add(v);
		if (!add) {
			final String message = zu.message();

			final String t = object.getClass().getSimpleName() + "." + field.getName();
			final String format = String.format(message, t, v);
			throw new ValidatedException(format, HttpStatusEnum.HTTP_400.getStatus());
		}

	}

	public static void validatedZLength(final Object object, final Field field) {
		final ZLength zl = field.getAnnotation(ZLength.class);
		if (zl == null) {
			return;
		}

		final Object v = ZValidator.getFieldValue(object, field);
		if (v == null) {
			ZValidator.throwZNotNullException(object, field);
			return;
		}

		if (!ZValidator.isString(v.getClass())) {
			throw new ValidatedException(
					"@" + ZLength.class.getSimpleName() + " 只能用于 String类型,当前用于字段[" + field.getName() + "]",
					HttpStatusEnum.HTTP_400.getStatus());
		}

		final String s = (String) v;
		if ((s.length() < zl.min()) || (s.length() > zl.max())) {

			final String message = zl.message();

			final String pName = field.isAnnotationPresent(ZValue.class)
					? "[" + field.getAnnotation(ZValue.class).name() + "]"
							: "";
			final String t = object.getClass().getSimpleName() + "." + field.getName();

			final String itemName = gItemName(object, field);

			final String format = String.format(message, t + pName, String.valueOf(zl.min()), String.valueOf(zl.max()),
					String.valueOf(s.length()))
					+ (STU.isEmpty(itemName) ? ""
							: ("\r\n\t" + "请配置[" + itemName + "]为在[" + zl.min() + "]和[" + zl.max() + "]之间"));
			throw new ValidatedException(format, HttpStatusEnum.HTTP_400.getStatus());
		}

	}

	public static void validatedZStartWith(final Object object, final Field field) {
		final ZStartWith startWidh = field.getAnnotation(ZStartWith.class);
		if (startWidh == null) {
			return;
		}

		final Class<?> type = field.getType();
		if (!type.getCanonicalName().equals(String.class.getCanonicalName())) {
			throw new ValidatedException(
					"@" + ZStartWith.class.getSimpleName() + " 只能用于 String类型,当前用于字段[" + field.getName() + "]",
					HttpStatusEnum.HTTP_400.getStatus());
		}

		final Object value = RU.getFiledValue(object, field);
		if (value == null) {
			ZValidator.throwZNotNullException(object, field);
		}

		final String v2 = String.valueOf(value);
		if (v2.isEmpty()) {
			ZValidator.throwZNotEmptyException(object, field);
		}

		final String prefix = startWidh.prefix();
		final boolean startsWith = v2.startsWith(prefix);
		if (!startsWith) {

			final String message = ZStartWith.MESSAGE;
			final String t = object.getClass().getSimpleName() + "." + field.getName();
			final String pName = field.isAnnotationPresent(ZValue.class)
					? "[" + field.getAnnotation(ZValue.class).name() + "]"
							: "";

			final String itemName = gItemName(object, field);

			final String format = String.format(message, t + pName, prefix)
					+ (STU.isEmpty(itemName) ? "" : ("\r\n\t" + "请配置[" + itemName + "]为以[" + prefix + "]开始"));

			throw new ValidatedException(format, HttpStatusEnum.HTTP_400.getStatus());
		}
	}

	public static void validatedZEndsWith(final Object object, final Field field) {
		final ZEndsWith endsWith = field.getAnnotation(ZEndsWith.class);
		if (endsWith == null) {
			return;
		}

		final Class<?> type = field.getType();
		if (!type.getCanonicalName().equals(String.class.getCanonicalName())) {
			throw new ValidatedException(
					"@" + ZStartWith.class.getSimpleName() + " 只能用于 String类型,当前用于字段[" + field.getName() + "]",
					HttpStatusEnum.HTTP_400.getStatus());
		}

		final Object value = RU.getFiledValue(object, field);
		if (value == null) {
			ZValidator.throwZNotNullException(object, field);
		}

		final String v2 = String.valueOf(value);
		if (v2.isEmpty()) {
			ZValidator.throwZNotEmptyException(object, field);
		}

		if (!v2.endsWith(endsWith.suffix())) {

			final String message = ZEndsWith.MESSAGE;
			final String t = object.getClass().getSimpleName() + "." + field.getName();
			final String pName = field.isAnnotationPresent(ZValue.class)
					? "[" + field.getAnnotation(ZValue.class).name() + "]"
							: "";

			final String itemName = gItemName(object, field);

			final String format = String.format(message, t + pName, endsWith.suffix())
					+ (STU.isEmpty(itemName) ? "" : ("\r\n\t" + "请配置[" + itemName + "]为以[" + endsWith.suffix() + "]结尾"));

			throw new ValidatedException(format, HttpStatusEnum.HTTP_400.getStatus());
		}
	}

	public static void validatedZMax(final Parameter p, final Object paramValue, final Object maxValue) {
		if (paramValue == null) {
			ZValidator.throwZNotNullException(p.getName());
			return;
		}

		final Class<? extends Object> pvClass = paramValue.getClass();
		if (!ZValidator.isZMinZMaxSupported(pvClass)) {
			throw new ValidatedException("@" + ZMin.class.getSimpleName()
					+ " 只能用于Byte,Short,Integer,Long,Float,Double,BigDecimal,BigInteger,AtomicLong,AtomicInteger类型,当前用于["
					+ p.getName() + "]", HttpStatusEnum.HTTP_400.getStatus());
		}

		final String canonicalName = pvClass.getCanonicalName();
		if (pvClass == Byte.class) {
			if (Byte.valueOf(String.valueOf(paramValue)) > ((Number) maxValue).byteValue()) {
				ZValidator.throwZMaxMessage(p.getName(), paramValue, ((Number) maxValue).byteValue());
			}
		} else if (pvClass == Short.class) {
			if (Short.valueOf(String.valueOf(paramValue)) > ((Number) maxValue).shortValue()) {
				ZValidator.throwZMaxMessage(p.getName(), paramValue, ((Number) maxValue).shortValue());
			}
		} else if (pvClass == Integer.class) {
			if (Integer.valueOf(String.valueOf(paramValue)) > ((Number) maxValue).intValue()) {
				ZValidator.throwZMaxMessage(p.getName(), paramValue, ((Number) maxValue).intValue());
			}
		} else if (pvClass == Long.class) {
			if (Long.valueOf(String.valueOf(paramValue)) > ((Number) maxValue).longValue()) {
				ZValidator.throwZMaxMessage(p.getName(), paramValue, ((Number) maxValue).longValue());
			}
		} else if (pvClass == Float.class) {
			if (Float.valueOf(String.valueOf(paramValue)) > ((Number) maxValue).floatValue()) {
				ZValidator.throwZMaxMessage(p.getName(), paramValue, ((Number) maxValue).floatValue());
			}
		} else if ((pvClass == Double.class)
				&& (Double.valueOf(String.valueOf(paramValue)) > ((Number) maxValue).doubleValue())) {
			ZValidator.throwZMaxMessage(p.getName(), paramValue, ((Number) maxValue).doubleValue());
		} else if (pvClass == BigInteger.class) {
			final BigInteger bi = (BigInteger) paramValue;
			if (bi.doubleValue() > ((BigInteger) maxValue).doubleValue()) {
				ZValidator.throwZMaxMessage(p.getName(), paramValue, maxValue);
			}
		} else if (pvClass == BigDecimal.class) {
			final BigDecimal bd = (BigDecimal) paramValue;
			if (bd.doubleValue() > ((BigDecimal) maxValue).doubleValue()) {
				ZValidator.throwZMaxMessage(p.getName(), paramValue, maxValue);
			}
		} else if (pvClass == AtomicInteger.class) {
			final AtomicInteger ai = (AtomicInteger) paramValue;
			if (ai.doubleValue() > ((AtomicInteger) maxValue).doubleValue()) {
				ZValidator.throwZMaxMessage(p.getName(), paramValue, maxValue);
			}
		} else if (pvClass == AtomicLong.class) {
			final AtomicLong al = (AtomicLong) paramValue;
			if (al.doubleValue() > ((AtomicLong) maxValue).decrementAndGet()) {
				ZValidator.throwZMaxMessage(p.getName(), paramValue, maxValue);
			}
		}
	}

	public static void validatedZMin(final Parameter p, final Object paramValue, final Object minValue) {
		if (paramValue == null) {
			ZValidator.throwZNotNullException(p.getName());
			return;
		}

		final Class<? extends Object> pvClass = paramValue.getClass();
		if (!ZValidator.isZMinZMaxSupported(pvClass)) {
			throw new ValidatedException("@" + ZMin.class.getSimpleName()
					+ " 只能用于Byte,Short,Integer,Long,Float,Double,BigDecimal,BigInteger,AtomicLong,AtomicInteger类型,当前用于["
					+ p.getName() + "]", HttpStatusEnum.HTTP_400.getStatus());
		}

		final String canonicalName = pvClass.getCanonicalName();
		if (pvClass == Byte.class) {
			if (Byte.valueOf(String.valueOf(paramValue)) < ((Number) minValue).byteValue()) {
				ZValidator.throwZMinMessage(p.getName(), paramValue, ((Number) minValue).byteValue());
			}
		} else if (pvClass == Short.class) {
			if (Short.valueOf(String.valueOf(paramValue)) < ((Number) minValue).shortValue()) {
				ZValidator.throwZMinMessage(p.getName(), paramValue, ((Number) minValue).shortValue());
			}
		} else if (pvClass == Integer.class) {
			if (Integer.valueOf(String.valueOf(paramValue)) < ((Number) minValue).intValue()) {
				ZValidator.throwZMinMessage(p.getName(), paramValue, ((Number) minValue).intValue());
			}
		} else if (pvClass == Long.class) {
			if (Long.valueOf(String.valueOf(paramValue)) < ((Number) minValue).longValue()) {
				ZValidator.throwZMinMessage(p.getName(), paramValue, ((Number) minValue).longValue());
			}
		} else if (pvClass == Float.class) {
			if (Float.valueOf(String.valueOf(paramValue)) < ((Number) minValue).floatValue()) {
				ZValidator.throwZMinMessage(p.getName(), paramValue, ((Number) minValue).floatValue());
			}
		} else if ((pvClass == Double.class)
				&& (Double.valueOf(String.valueOf(paramValue)) < ((Number) minValue).doubleValue())) {
			ZValidator.throwZMinMessage(p.getName(), paramValue, ((Number) minValue).doubleValue());
		} else if (pvClass == BigInteger.class) {
			final BigInteger bi = (BigInteger) paramValue;
			if (bi.doubleValue() < ((BigInteger) minValue).doubleValue()) {
				ZValidator.throwZMinMessage(p.getName(), paramValue, minValue);
			}
		} else if (pvClass == BigDecimal.class) {
			final BigDecimal bd = (BigDecimal) paramValue;
			if (bd.doubleValue() < ((BigDecimal) minValue).doubleValue()) {
				ZValidator.throwZMinMessage(p.getName(), paramValue, minValue);
			}
		} else if (pvClass == AtomicInteger.class) {
			final AtomicInteger ai = (AtomicInteger) paramValue;
			if (ai.doubleValue() < ((AtomicInteger) minValue).doubleValue()) {
				ZValidator.throwZMinMessage(p.getName(), paramValue, minValue);
			}
		} else if (pvClass == AtomicLong.class) {
			final AtomicLong al = (AtomicLong) paramValue;
			if (al.doubleValue() < ((AtomicLong) minValue).decrementAndGet()) {
				ZValidator.throwZMinMessage(p.getName(), paramValue, minValue);
			}
		}
	}

	public static void validatedZMin(final Object object, final Field field) {
		final ZMin zMin = field.getAnnotation(ZMin.class);
		if (zMin == null) {
			return;
		}

		final double min = zMin.min();

		final Object minFiledValue = RU.getFiledValue(object, field);
		if (minFiledValue == null) {
			ZValidator.throwZNotNullException(object, field);
			return;
		}

		final Class<? extends Object> fvClass = minFiledValue.getClass();
		if (!ZValidator.isZMinZMaxSupported(fvClass)) {
			throw new ValidatedException("@" + ZMin.class.getSimpleName()
					+ " 只能用于Byte,Short,Integer,Long,Float,Double,BigDecimal,BigInteger,AtomicLong,AtomicInteger类型,当前用于字段["
					+ field.getName() + "]", HttpStatusEnum.HTTP_400.getStatus());
		}

		final String canonicalName = fvClass.getCanonicalName();
		if (fvClass == Byte.class) {
			if (Byte.valueOf(String.valueOf(minFiledValue)) < min) {
				ZValidator.throwZMinMessage(object, field, (byte) min, minFiledValue);
			}
		} else if (fvClass == Short.class) {
			if (Short.valueOf(String.valueOf(minFiledValue)) < min) {
				ZValidator.throwZMinMessage(object, field, (short) min, minFiledValue);
			}
		} else if (fvClass == Integer.class) {
			if (Integer.valueOf(String.valueOf(minFiledValue)) < min) {
				ZValidator.throwZMinMessage(object, field, (int) min, minFiledValue);
			}
		} else if (fvClass == Long.class) {
			if (Long.valueOf(String.valueOf(minFiledValue)) < min) {
				ZValidator.throwZMinMessage(object, field, (long) min, minFiledValue);
			}
		} else if (fvClass == Float.class) {
			if (Float.valueOf(String.valueOf(minFiledValue)) < min) {
				ZValidator.throwZMinMessage(object, field, min, minFiledValue);
			}
		} else if ((fvClass == Double.class)
				&& (Double.valueOf(String.valueOf(minFiledValue)) < min)) {
			ZValidator.throwZMinMessage(object, field, min, minFiledValue);
		} else if (fvClass == BigInteger.class) {
			final BigInteger bi = (BigInteger) minFiledValue;
			if (bi.doubleValue() < min) {
				ZValidator.throwZMinMessage(object, field, min, minFiledValue);
			}
		} else if (fvClass == BigDecimal.class) {
			final BigDecimal bd = (BigDecimal) minFiledValue;
			if (bd.doubleValue() < min) {
				ZValidator.throwZMinMessage(object, field, min, minFiledValue);
			}
		} else if (fvClass == AtomicInteger.class) {
			final AtomicInteger ai = (AtomicInteger) minFiledValue;
			if (ai.doubleValue() < min) {
				ZValidator.throwZMinMessage(object, field, min, minFiledValue);
			}
		} else if (fvClass == AtomicLong.class) {
			final AtomicLong al = (AtomicLong) minFiledValue;
			if (al.doubleValue() < min) {
				ZValidator.throwZMinMessage(object, field, min, minFiledValue);
			}
		}

	}

	public static void validatedZNotEmpty(final Object object, final Field field) {
		final ZNotEmtpy nn = field.getAnnotation(ZNotEmtpy.class);
		if (nn == null) {
			return;
		}

		final Object value = RU.getFiledValue(object, field);
		if (value == null) {
			ZValidator.throwZNotNullException(object, field);
			return;
		}

		if ((value instanceof List) || (value instanceof Set)) {
			if (((Collection) value).isEmpty()) {
				ZValidator.throwZNotEmptyException(object, field);
			}
		} else if (value instanceof Map) {
			if (((Map) value).isEmpty()) {
				ZValidator.throwZNotEmptyException(object, field);
			}
		} else if (ZValidator.isString(value.getClass())) {
			// 此处不内联，防止自动保存 两个条件放在了一个if里，导致后续添加else分支时混乱
			final String string = (String) value;
			if (string.isEmpty()) {
				ZValidator.throwZNotEmptyException(object, field);
			}
		}

	}

	private static void throwZNotEmptyException(final Object object, final Field field) {
		final String message = ZNotEmtpy.MESSAGE;
		final String t = object.getClass().getSimpleName() + "." + field.getName();

		final String pName = field.isAnnotationPresent(ZValue.class)
				? "[" + field.getAnnotation(ZValue.class).name() + "]"
						: "";

		final String itemName = gItemName(object, field);

		final String format = String.format(message, t + pName)
				+ (STU.isEmpty(itemName) ? "" : ("\r\n\t" + "请配置[" + itemName + "]为非empty值"));
		throw new ValidatedException(format, HttpStatusEnum.HTTP_400.getStatus());
	}

	public static void validatedZMax(final Object object, final Field field) {
		final ZMax zMax = field.getAnnotation(ZMax.class);
		if (zMax == null) {
			return;
		}

		final double max = zMax.max();

		final Object maxFiledValue = RU.getFiledValue(object, field);
		if (maxFiledValue == null) {
			ZValidator.throwZNotNullException(object, field);
			return;
		}

		final Class<? extends Object> fvClass = maxFiledValue.getClass();
		if (!ZValidator.isZMinZMaxSupported(fvClass)) {
			throw new ValidatedException("@" + ZMax.class.getSimpleName()
					+ " 只能用于Byte,Short,Integer,Long,Float,Double,BigDecimal,BigInteger,AtomicLong,AtomicInteger类型,当前用于字段["
					+ field.getName() + "]");
		}

		final String canonicalName = fvClass.getCanonicalName();
		if (fvClass == Byte.class) {
			if (Byte.valueOf(String.valueOf(maxFiledValue)) > max) {
				ZValidator.throwZMaxMessage(object, field, (byte) max, maxFiledValue);
			}
		} else if (fvClass == Short.class) {
			if (Short.valueOf(String.valueOf(maxFiledValue)) > max) {
				ZValidator.throwZMaxMessage(object, field, (short) max, maxFiledValue);
			}
		} else if (fvClass == Integer.class) {
			if (Integer.valueOf(String.valueOf(maxFiledValue)) > max) {
				ZValidator.throwZMaxMessage(object, field, (int) max, maxFiledValue);
			}
		} else if (fvClass == Long.class) {
			if (Long.valueOf(String.valueOf(maxFiledValue)) > max) {
				ZValidator.throwZMaxMessage(object, field, (long) max, maxFiledValue);
			}
		} else if (fvClass == Float.class) {
			if (Float.valueOf(String.valueOf(maxFiledValue)) > max) {
				ZValidator.throwZMaxMessage(object, field, max, maxFiledValue);
			}
		} else if ((fvClass == Double.class)
				&& (Double.valueOf(String.valueOf(maxFiledValue)) > max)) {
			ZValidator.throwZMaxMessage(object, field, max, maxFiledValue);
		} else if (fvClass == BigInteger.class) {
			final BigInteger bi = (BigInteger) maxFiledValue;
			if (bi.doubleValue() > max) {
				ZValidator.throwZMaxMessage(object, field, max, maxFiledValue);
			}
		} else if (fvClass == BigDecimal.class) {
			final BigDecimal bd = (BigDecimal) maxFiledValue;
			if (bd.doubleValue() > max) {
				ZValidator.throwZMaxMessage(object, field, max, maxFiledValue);
			}
		} else if (fvClass == AtomicInteger.class) {
			final AtomicInteger ai = (AtomicInteger) maxFiledValue;
			if (ai.doubleValue() > max) {
				ZValidator.throwZMaxMessage(object, field, max, maxFiledValue);
			}
		} else if (fvClass == AtomicLong.class) {
			final AtomicLong al = (AtomicLong) maxFiledValue;
			if (al.doubleValue() > max) {
				ZValidator.throwZMaxMessage(object, field, max, maxFiledValue);
			}
		}

	}

	public static void validatedZCustom(final Object object, final Field field) throws Exception  {
		final ZCustom zc = field.getAnnotation(ZCustom.class);
		if (zc == null) {
			return;
		}

		if (zc.ignoreNull()) {
			final Object v = RU.getFiledValue(object, field);
			if (v == null) {
				return;
			}
		}

		final Class<? extends ZCustomValidator> cls = zc.cls();

		final ZCustomValidator customValidator = ZSingleton.getSingletonByClass(cls);
		if (customValidator != null) {
			try {
				customValidator.validated(object, field);
			} catch (final RuntimeException e) {
				throw e;
//				if (e instanceof ValidatedException) {
//					throw (ValidatedException) e;
//				}
//
//				throw new ValidatedException(Task.gExceptionMessage(e));
			}
		}

	}

	public static void validatedAll(final Object object, final Field field) throws Exception {

		ZValidator.validatedZNotNull(object, field);
		ZValidator.validatedZNotEmpty(object, field);
		ZValidator.validatedZLength(object, field);
		ZValidator.validatedZMin(object, field);
		ZValidator.validatedZMax(object, field);
		ZValidator.validatedZStartWith(object, field);
		ZValidator.validatedZEndsWith(object, field);
		ZValidator.validatedZPositive(object, field);
		ZValidator.validatedZUnique(object, field);
		ZValidator.validatedZCustom(object, field);

	}

	private static Object getFieldValue(final Object object, final Field field) {
		return RU.getFiledValue(object, field);
	}

	private static void throwZMaxMessage(final String paramName, final Object maxFiledValue, final Object maxValue) {
		final String message = ZMax.MESSAGE;
		final String t = paramName;
		final String format = String.format(message, t, maxValue, maxFiledValue);
		throw new ValidatedException(format);
	}

	private static void throwZMinMessage(final String paramName, final Object minFiledValue, final Object minValue) {
		final String message = ZMin.MESSAGE;
		final String t = paramName;
		final String format = String.format(message, t, minValue, minFiledValue);
		throw new ValidatedException(format);
	}

	private static void throwZMinMessage(final Object object, final Field field, final Object min,
			final Object minFiledValue) {
		final String message = ZMin.MESSAGE;
		final String pName = field.isAnnotationPresent(ZValue.class)
				? "[" + field.getAnnotation(ZValue.class).name() + "]"
						: "";
		final String t = object.getClass().getSimpleName() + "." + field.getName();

		final String itemName = gItemName(object, field);

		final String format = String.format(message, t + pName, min, minFiledValue)
				+ (STU.isEmpty(itemName) ? "" : ("\r\n\t" + "请配置[" + itemName + "]为不小于[" + min + "]"));
		throw new ValidatedException(format);
	}

	private static void throwZNotNullException(final String paramName) {
		final String message = ZNotNull.MESSAGE;
		final String t = paramName;
		final String format = String.format(message, t);
		throw new ValidatedException(format);
	}

	private static void throwZNotNullException(final Object object, final Field field) {
		final String message = ZNotNull.MESSAGE;
		final String t = object.getClass().getSimpleName() + "." + field.getName();

		final String pName = field.isAnnotationPresent(ZValue.class)
				? "[" + field.getAnnotation(ZValue.class).name() + "]"
						: "";

		final String itemName = gItemName(object, field);

		final String format = String.format(message, t + pName)
				+ (STU.isEmpty(itemName) ? "" : ("\r\n\t" + "请配置[" + itemName + "]"));
		throw new ValidatedException(format, HttpStatusEnum.HTTP_400.getStatus());
	}

	/**
	 * 	根据对象和字段生成一个配置项，只有用了 @ZConfigurationProperties 注解的配置类
	 *  本方法才会放回如[xx.xx]的String，用于提示配置项[xx.xx]如何配置
	 *
	 *  否则返回""，因为可能是接口中声明比如 ( @ZRequestBody @ZValidated final PO po)
	 *  的自定义对象来验证的，只提示值不符合规则就行了
	 *
	 * @param object
	 * @param field
	 * @return
	 */
	// FIXME 2026年1月3日 23:12:09 zhangzhen :  这个方法有问题，@ZCP.prefix未配置时，提示未[.key]，记得去掉.
	private static String gItemName(final Object object, final Field field) {
		final ZConfigurationProperties zcp = object.getClass().getAnnotation(ZConfigurationProperties.class);
		if (zcp == null) {
			return "";
		}

		final ZValue zValue = field.getAnnotation(ZValue.class);
		if (zValue != null) {
			return zValue.name();
		}

		final String prefix = zcp.prefix().endsWith(".") ? zcp.prefix() : zcp.prefix() + ".";
		final String name = ZConfigurationPropertiesScanner.convert(field.getName());

		return prefix + name;
	}

	private static void throwZMaxMessage(final Object object, final Field field, final Object max,
			final Object maxFiledValue) {
		final String message = ZMax.MESSAGE;
		final String pName = field.isAnnotationPresent(ZValue.class)
				? "[" + field.getAnnotation(ZValue.class).name() + "]"
						: "";

		final String t = object.getClass().getSimpleName() + "." + field.getName();

		final String itemName = gItemName(object, field);

		final String format = String.format(message, t + pName, max, maxFiledValue)
				+ (STU.isEmpty(itemName) ? "" : ("\r\n\t" + "请配置[" + itemName + "]为不大于[" + max + "]"));
		throw new ValidatedException(format, HttpStatusEnum.HTTP_400.getStatus());
	}

	public static final Set<String> BAOHAN = Set.of("vo.zframework.configuration",
			"vo.zframework.configuration.properties");

	public static final Set<String> TIAO_GUO = Set.of(
				"vo.log",
				"vo.log.common",
				"vo.log.conf",
				"vo.log.core",
				"vo.log.enums",
				"vo.log.handler",

				"vo.zframework.anno",
				"vo.zframework.aop",
				"vo.zframework.api",
				"vo.zframework.cache",
				"vo.zframework.common",
				"vo.zframework.compression",

				"vo.zframework.dynamic",
				"vo.zframework.email",
				"vo.zframework.enums",
				"vo.zframework.event",
				"vo.zframework.exception",
				"vo.zframework.html",
				"vo.zframework.http",
				"vo.zframework.protobuf",
				"vo.zframework.scanner",
				"vo.zframework.template",
				"vo.zframework.validator",
				"vo.zframework.zclass"

		);


	/**
	 * 程序启动时调用此方法，扫描所有带有校验注解的字段，来判断此字段是否支持
	 *
	 * @param packageName
	 *
	 */
	public static void start(final String... packageName) {
		final Set<Class<?>> clsSet = ClassMap.scanPackage(packageName);

		final List<Class<?>> list = clsSet.parallelStream()
				.filter(cls -> BAOHAN.contains(cls.getPackageName()))
				.collect(Collectors.toList());

		for (final Class<?> cls : list) {
			final Field[] fs = cls.getDeclaredFields();
			for (final Field f : fs) {
				final Annotation[] as = f.getDeclaredAnnotations();
				for (final Annotation annotation : as) {
					final Class<? extends Annotation> annotationType = annotation.annotationType();
					if (!ZValidator.isValidatorAnnotation(annotationType)) {
						continue;
					}

					if (annotationType == ZNotNull.class) {
						// @ZNotNull 不用校验，因为它支持所有类型
					} else if ((annotationType == ZNotEmtpy.class) && !ZValidator.isZNotEmptySupported(f.getType())) {
						ZValidator.throwTypeNotSupportedExcpetion(cls, ZNotEmtpy.class, f);
					} else if ((annotationType == ZMin.class) && !ZValidator.isZMinZMaxSupported(f.getType())) {
						ZValidator.throwTypeNotSupportedExcpetion(cls, ZMin.class, f);
					} else if ((annotationType == ZMax.class) && !ZValidator.isZMinZMaxSupported(f.getType())) {
						ZValidator.throwTypeNotSupportedExcpetion(cls, ZMax.class, f);
					} else if ((annotationType == ZLength.class) && !ZValidator.isString(f.getType())) {
						ZValidator.throwTypeNotSupportedExcpetion(cls, ZLength.class, f);
					} else if ((annotationType == ZStartWith.class) && !ZValidator.isString(f.getType())) {
						ZValidator.throwTypeNotSupportedExcpetion(cls, ZStartWith.class, f);
					} else if ((annotationType == ZEndsWith.class) && !ZValidator.isString(f.getType())) {
						ZValidator.throwTypeNotSupportedExcpetion(cls, ZEndsWith.class, f);
					} else if ((annotationType == ZPositive.class) && !ZValidator.isZMinZMaxSupported(f.getType())) {
						ZValidator.throwTypeNotSupportedExcpetion(cls, ZPositive.class, f);
					} else if (annotationType == ZCustom.class) {

						if (!ZValidator.isZCustomSupported(f.getType())) {
							ZValidator.throwTypeNotSupportedExcpetion(cls, ZCustom.class, f);
						}

						final Class<? extends ZCustomValidator> customClass = f.getAnnotation(ZCustom.class).cls();
						try {
							ZSingleton.getSingletonByClass(customClass);
						} catch (final Exception e) {
							final String message = Task.gExceptionMessage(e);
							throw new ValidatedException("@" + ZCustom.class.getSimpleName() + ".cls 指定的类型["
									+ customClass + "]初始化异常,message=" + message,HttpStatusEnum.HTTP_400.getStatus());
						}

					}

				}

			}
		}

	}

	public static void throwTypeNotSupportedExcpetion(final Class<?> cls, final Class<? extends Annotation> annoCls,
			final Field f) {
		throw new TypeNotSupportedExcpetion(cls.getName() + "." + f.getName() + "类型为" + f.getType().getSimpleName()
				+ ",校验注解为" + "@" + annoCls.getSimpleName());
	}

	public static boolean isZNotEmptySupported(final Class<?> annoClass) {
		return (annoClass == String.class) || (annoClass == List.class) || (annoClass == Set.class) || (annoClass == Map.class);
	}

	public static boolean isValidatorAnnotation(final Class<? extends Annotation> annoClass) {
		return ZValidator.VA_SET.contains(annoClass);
	}

}
