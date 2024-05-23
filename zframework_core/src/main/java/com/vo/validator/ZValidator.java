package com.vo.validator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.Sets;
import com.vo.anno.ZValue;
import com.vo.core.Task;
import com.vo.core.ZSingleton;
import com.vo.exception.TypeNotSupportedExcpetion;
import com.vo.exception.ValidatedException;
import com.vo.scanner.ClassMap;

/**
 * 验证器
 *
 * @author zhangzhen
 * @date 2023年10月15日
 *
 */
public class ZValidator {

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
					+ p.getName() + "]");
		}

		final double doubleValue = ((Number) value).doubleValue();
		// FIXME 2023年11月1日 下午7:10:13 zhanghen: XXX 待定 ((Number) v).doubleValue() 是否可行
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
		}

		if (!ZValidator.isZMinZMaxSupported(v.getClass())) {
			throw new ValidatedException("@" + ZPositive.class.getSimpleName()
					+ " 只能用于Byte,Short,Integer,Long,Float,Double,BigDecimal,BigInteger,AtomicLong,AtomicInteger类型,当前用于字段["
					+ field.getName() + "]");
		}

		final double doubleValue = ((Number) v).doubleValue();
		// FIXME 2023年11月1日 下午7:10:13 zhanghen: XXX 待定 ((Number) v).doubleValue() 是否可行
		if (doubleValue <= 0D) {
			final String message = ZPositive.MESSAGE;
			final String t = object.getClass().getSimpleName() + "." + field.getName();
			final String pName = field.isAnnotationPresent(ZValue.class)
					? "[" + field.getAnnotation(ZValue.class).name() + "]"
					: "";
			final String format = String.format(message, t + pName, v);

			throw new ValidatedException(format);
		}

	}

	public static boolean isString(final Class cls) {
		return cls == String.class;
	}

	public static boolean isZCustomSupported(final Class<?> cls) {
		// @ZCustom 支持任何类型
		// FIXME 2023年11月14日 下午10:22:03 zhanghen: TODO 要不要判断并提示不能使基本类型？
		return true;
	}

	public static boolean isZMinZMaxSupported(final Class<?> cls) {
		return (cls == Byte.class) || (cls == Short.class) || (cls == Integer.class) || (cls == Long.class)
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
			throw new ValidatedException(format);
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
		}

		if (!ZValidator.isString(v.getClass())) {
			throw new ValidatedException(
					"@" + ZLength.class.getSimpleName() + " 只能用于 String类型,当前用于字段[" + field.getName() + "]");
		}

		final String s = (String) v;
		if ((s.length() < zl.min()) || (s.length() > zl.max())) {

			final String message = zl.message();
//			final String message = ZLength.MESSAGE_DEFAULT.equals(zl.message())
//					? ZContext.getBean(ZLengthMessageConfigurationProperties.class).getMessage()
//					: zl.message();

			final String pName = field.isAnnotationPresent(ZValue.class)
					? "[" + field.getAnnotation(ZValue.class).name() + "]"
					: "";
			final String t = object.getClass().getSimpleName() + "." + field.getName();
			final String format = String.format(message, t + pName, String.valueOf(zl.min()), String.valueOf(zl.max()),
					String.valueOf(s.length()));
			throw new ValidatedException(format);
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
					"@" + ZStartWith.class.getSimpleName() + " 只能用于 String类型,当前用于字段[" + field.getName() + "]");
		}

		try {
			field.setAccessible(true);
			final Object value = field.get(object);
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
				final String format = String.format(message, t + pName, prefix);

				throw new ValidatedException(format);
			}

		} catch (final IllegalAccessException e) {
			e.printStackTrace();
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
					"@" + ZStartWith.class.getSimpleName() + " 只能用于 String类型,当前用于字段[" + field.getName() + "]");
		}

		try {
			field.setAccessible(true);
			final Object value = field.get(object);
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
				final String format = String.format(message, t + pName, endsWith.suffix());

				throw new ValidatedException(format);
			}

		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public static void validatedZMin(final Parameter p, final Object paramValue, final Object minValue) {
		if (paramValue == null) {
			ZValidator.throwZNotNullException(p.getName());
			return;
		}

		if (!ZValidator.isZMinZMaxSupported(paramValue.getClass())) {
			throw new ValidatedException("@" + ZMin.class.getSimpleName()
					+ " 只能用于Byte,Short,Integer,Long,Float,Double,BigDecimal,BigInteger,AtomicLong,AtomicInteger类型,当前用于["
					+ p.getName() + "]");
		}

		final String canonicalName = paramValue.getClass().getCanonicalName();
		if (canonicalName.equals(Byte.class.getCanonicalName())) {
			if (Byte.valueOf(String.valueOf(paramValue)) < ((Number) minValue).byteValue()) {
				ZValidator.throwZMinMessage(p.getName(), paramValue, ((Number) minValue).byteValue());
			}
		} else if (canonicalName.equals(Short.class.getCanonicalName())) {
			if (Short.valueOf(String.valueOf(paramValue)) < ((Number) minValue).shortValue()) {
				ZValidator.throwZMinMessage(p.getName(), paramValue, ((Number) minValue).shortValue());
			}
		} else if (canonicalName.equals(Integer.class.getCanonicalName())) {
			if (Integer.valueOf(String.valueOf(paramValue)) < ((Number) minValue).intValue()) {
				ZValidator.throwZMinMessage(p.getName(), paramValue, ((Number) minValue).intValue());
			}
		} else if (canonicalName.equals(Long.class.getCanonicalName())) {
			if (Long.valueOf(String.valueOf(paramValue)) < ((Number) minValue).longValue()) {
				ZValidator.throwZMinMessage(p.getName(), paramValue, ((Number) minValue).longValue());
			}
		} else if (canonicalName.equals(Float.class.getCanonicalName())) {
			if (Float.valueOf(String.valueOf(paramValue)) < ((Number) minValue).floatValue()) {
				ZValidator.throwZMinMessage(p.getName(), paramValue, ((Number) minValue).floatValue());
			}
		} else if (canonicalName.equals(Double.class.getCanonicalName())
				&& (Double.valueOf(String.valueOf(paramValue)) < ((Number) minValue).doubleValue())) {
			ZValidator.throwZMinMessage(p.getName(), paramValue, ((Number) minValue).doubleValue());
		} else if (canonicalName.equals(BigInteger.class.getCanonicalName())) {
			final BigInteger bi = (BigInteger) paramValue;
			if (bi.doubleValue() < ((BigInteger) minValue).doubleValue()) {
				ZValidator.throwZMinMessage(p.getName(), paramValue, minValue);
			}
		} else if (canonicalName.equals(BigDecimal.class.getCanonicalName())) {
			final BigDecimal bd = (BigDecimal) paramValue;
			if (bd.doubleValue() < ((BigDecimal) minValue).doubleValue()) {
				ZValidator.throwZMinMessage(p.getName(), paramValue, minValue);
			}
		} else if (canonicalName.equals(AtomicInteger.class.getCanonicalName())) {
			final AtomicInteger ai = (AtomicInteger) paramValue;
			if (ai.doubleValue() < ((AtomicInteger) minValue).doubleValue()) {
				ZValidator.throwZMinMessage(p.getName(), paramValue, minValue);
			}
		} else if (canonicalName.equals(AtomicLong.class.getCanonicalName())) {
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

		try {
			field.setAccessible(true);
			final Object minFiledValue = field.get(object);

			if (minFiledValue == null) {
				ZValidator.throwZNotNullException(object, field);
			}

			if (!ZValidator.isZMinZMaxSupported(minFiledValue.getClass())) {
				throw new ValidatedException("@" + ZMin.class.getSimpleName()
						+ " 只能用于Byte,Short,Integer,Long,Float,Double,BigDecimal,BigInteger,AtomicLong,AtomicInteger类型,当前用于字段["
						+ field.getName() + "]");
			}

			final String canonicalName = minFiledValue.getClass().getCanonicalName();
			if (canonicalName.equals(Byte.class.getCanonicalName())) {
				if (Byte.valueOf(String.valueOf(minFiledValue)) < min) {
					ZValidator.throwZMinMessage(object, field, (byte) min, minFiledValue);
				}
			} else if (canonicalName.equals(Short.class.getCanonicalName())) {
				if (Short.valueOf(String.valueOf(minFiledValue)) < min) {
					ZValidator.throwZMinMessage(object, field, (short) min, minFiledValue);
				}
			} else if (canonicalName.equals(Integer.class.getCanonicalName())) {
				if (Integer.valueOf(String.valueOf(minFiledValue)) < min) {
					ZValidator.throwZMinMessage(object, field, (int) min, minFiledValue);
				}
			} else if (canonicalName.equals(Long.class.getCanonicalName())) {
				if (Long.valueOf(String.valueOf(minFiledValue)) < min) {
					ZValidator.throwZMinMessage(object, field, (long) min, minFiledValue);
				}
			} else if (canonicalName.equals(Float.class.getCanonicalName())) {
				if (Float.valueOf(String.valueOf(minFiledValue)) < min) {
					ZValidator.throwZMinMessage(object, field, min, minFiledValue);
				}
			} else if (canonicalName.equals(Double.class.getCanonicalName())
					&& (Double.valueOf(String.valueOf(minFiledValue)) < min)) {
				ZValidator.throwZMinMessage(object, field, min, minFiledValue);
			} else if (canonicalName.equals(BigInteger.class.getCanonicalName())) {
				final BigInteger bi = (BigInteger) minFiledValue;
				if (bi.doubleValue() < min) {
					ZValidator.throwZMinMessage(object, field, min, minFiledValue);
				}
			} else if (canonicalName.equals(BigDecimal.class.getCanonicalName())) {
				final BigDecimal bd = (BigDecimal) minFiledValue;
				if (bd.doubleValue() < min) {
					ZValidator.throwZMinMessage(object, field, min, minFiledValue);
				}
			} else if (canonicalName.equals(AtomicInteger.class.getCanonicalName())) {
				final AtomicInteger ai = (AtomicInteger) minFiledValue;
				if (ai.doubleValue() < min) {
					ZValidator.throwZMinMessage(object, field, min, minFiledValue);
				}
			} else if (canonicalName.equals(AtomicLong.class.getCanonicalName())) {
				final AtomicLong al = (AtomicLong) minFiledValue;
				if (al.doubleValue() < min) {
					ZValidator.throwZMinMessage(object, field, min, minFiledValue);
				}
			}
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		}

	}

	public static void validatedZNotEmpty(final Object object, final Field field) {
		final ZNotEmtpy nn = field.getAnnotation(ZNotEmtpy.class);
		if (nn == null) {
			return;
		}

		field.setAccessible(true);
		try {
			final Object value = field.get(object);
			if (value == null) {
				ZValidator.throwZNotNullException(object, field);
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

		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		}

	}

	private static void throwZNotEmptyException(final Object object, final Field field) {
		final String message = ZNotEmtpy.MESSAGE;
		final String t = object.getClass().getSimpleName() + "." + field.getName();

		final String pName = field.isAnnotationPresent(ZValue.class)
				? "[" + field.getAnnotation(ZValue.class).name() + "]"
				: "";

		final String format = String.format(message, t + pName);
		throw new ValidatedException(format);
	}

	public static void validatedZMax(final Object object, final Field field) {
		final ZMax zMax = field.getAnnotation(ZMax.class);
		if (zMax == null) {
			return;
		}

		final double max = zMax.max();

		try {
			field.setAccessible(true);
			final Object maxFiledValue = field.get(object);

			if (maxFiledValue == null) {
				ZValidator.throwZNotNullException(object, field);
			}

			if (!ZValidator.isZMinZMaxSupported(maxFiledValue.getClass())) {
				throw new ValidatedException("@" + ZMax.class.getSimpleName()
						+ " 只能用于Byte,Short,Integer,Long,Float,Double,BigDecimal,BigInteger,AtomicLong,AtomicInteger类型,当前用于字段["
						+ field.getName() + "]");
			}

			final String canonicalName = maxFiledValue.getClass().getCanonicalName();
			if (canonicalName.equals(Byte.class.getCanonicalName())) {
				if (Byte.valueOf(String.valueOf(maxFiledValue)) > max) {
					ZValidator.throwZMaxMessage(object, field, (byte) max, maxFiledValue);
				}
			} else if (canonicalName.equals(Short.class.getCanonicalName())) {
				if (Short.valueOf(String.valueOf(maxFiledValue)) > max) {
					ZValidator.throwZMaxMessage(object, field, (short) max, maxFiledValue);
				}
			} else if (canonicalName.equals(Integer.class.getCanonicalName())) {
				if (Integer.valueOf(String.valueOf(maxFiledValue)) > max) {
					ZValidator.throwZMaxMessage(object, field, (int) max, maxFiledValue);
				}
			} else if (canonicalName.equals(Long.class.getCanonicalName())) {
				if (Long.valueOf(String.valueOf(maxFiledValue)) > max) {
					ZValidator.throwZMaxMessage(object, field, (long) max, maxFiledValue);
				}
			} else if (canonicalName.equals(Float.class.getCanonicalName())) {
				if (Float.valueOf(String.valueOf(maxFiledValue)) > max) {
					ZValidator.throwZMaxMessage(object, field, max, maxFiledValue);
				}
			} else if (canonicalName.equals(Double.class.getCanonicalName())
					&& (Double.valueOf(String.valueOf(maxFiledValue)) > max)) {
				ZValidator.throwZMaxMessage(object, field, max, maxFiledValue);
			} else if (canonicalName.equals(BigInteger.class.getCanonicalName())) {
				final BigInteger bi = (BigInteger) maxFiledValue;
				if (bi.doubleValue() > max) {
					ZValidator.throwZMaxMessage(object, field, max, maxFiledValue);
				}
			} else if (canonicalName.equals(BigDecimal.class.getCanonicalName())) {
				final BigDecimal bd = (BigDecimal) maxFiledValue;
				if (bd.doubleValue() > max) {
					ZValidator.throwZMaxMessage(object, field, max, maxFiledValue);
				}
			} else if (canonicalName.equals(AtomicInteger.class.getCanonicalName())) {
				final AtomicInteger ai = (AtomicInteger) maxFiledValue;
				if (ai.doubleValue() > max) {
					ZValidator.throwZMaxMessage(object, field, max, maxFiledValue);
				}
			} else if (canonicalName.equals(AtomicLong.class.getCanonicalName())) {
				final AtomicLong al = (AtomicLong) maxFiledValue;
				if (al.doubleValue() > max) {
					ZValidator.throwZMaxMessage(object, field, max, maxFiledValue);
				}
			}
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		}

	}

	public static void validatedZCustom(final Object object, final Field field) {
		final ZCustom zc = field.getAnnotation(ZCustom.class);
		if (zc == null) {
			return;
		}

		final Class<? extends ZCustomValidator> cls = zc.cls();

		final ZCustomValidator customValidator = ZSingleton.getSingletonByClass(cls);
		if (customValidator != null) {
			try {
				customValidator.validated(object, field);
			} catch (final Exception e) {
				if (e instanceof ValidatedException) {
					throw (ValidatedException) e;
				}

				throw new ValidatedException(Task.gExceptionMessage(e));
			}
		}

	}

	public static void validatedAll(final Object object, final Field field) {
		// FIXME 2023年10月31日 下午10:13:39 zhanghen: 启动时是否验证注解值的合理性，如下声明：
//		@ZStartWith(prefix = "ZH")
//		@ZEndsWith(suffix = "G")
//		@ZLength(max = 3)
//		private String name;

		// 显然是不合理，name怎么传值都不会通过验证。该怎么办？程序启动时就验证所有的注解的组合中不合理的情况？

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
		try {
			field.setAccessible(true);
			final Object v = field.get(object);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
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
		final String format = String.format(message, t + pName, min, minFiledValue);
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

		final String format = String.format(message, t + pName);
		throw new ValidatedException(format);
	}

	private static void throwZMaxMessage(final Object object, final Field field, final Object max,
			final Object maxFiledValue) {
		final String message = ZMax.MESSAGE;
		final String pName = field.isAnnotationPresent(ZValue.class)
				? "[" + field.getAnnotation(ZValue.class).name() + "]"
				: "";
		final String t = object.getClass().getSimpleName() + "." + field.getName();
		final String format = String.format(message, t + pName, max, maxFiledValue);
		throw new ValidatedException(format);
	}

	/**
	 * 程序启动时调用此方法，扫描所有带有校验注解的字段，来判断此字段是否支持
	 *
	 * @param packageName
	 *
	 */
	public static void start(final String... packageName) {
		final Set<Class<?>> clsSet = ClassMap.scanPackage(packageName);
		for (final Class<?> cls : clsSet) {
			final Field[] fs = cls.getDeclaredFields();
			for (final Field f : fs) {
				final Annotation[] as = f.getDeclaredAnnotations();
				for (final Annotation annotation : as) {
					final boolean isVA = ZValidator.isValidatorAnnotation(annotation.annotationType());
					if (!isVA) {
						continue;
					}
					if (annotation.annotationType() == ZNotNull.class) {
						// @ZNotNull 不用校验，因为它支持所有类型
					} else if ((annotation.annotationType() == ZNotEmtpy.class) && !ZValidator.isZNotEmptySupported(f.getType())) {
						ZValidator.throwTypeNotSupportedExcpetion(cls, ZNotEmtpy.class, f);
					} else if ((annotation.annotationType() == ZMin.class) && !ZValidator.isZMinZMaxSupported(f.getType())) {
						ZValidator.throwTypeNotSupportedExcpetion(cls, ZMin.class, f);
					} else if ((annotation.annotationType() == ZMax.class) && !ZValidator.isZMinZMaxSupported(f.getType())) {
						ZValidator.throwTypeNotSupportedExcpetion(cls, ZMax.class, f);
					} else if ((annotation.annotationType() == ZLength.class) && !ZValidator.isString(f.getType())) {
						ZValidator.throwTypeNotSupportedExcpetion(cls, ZLength.class, f);
					} else if ((annotation.annotationType() == ZStartWith.class) && !ZValidator.isString(f.getType())) {
						ZValidator.throwTypeNotSupportedExcpetion(cls, ZStartWith.class, f);
					} else if ((annotation.annotationType() == ZEndsWith.class) && !ZValidator.isString(f.getType())) {
						ZValidator.throwTypeNotSupportedExcpetion(cls, ZEndsWith.class, f);
					} else if ((annotation.annotationType() == ZPositive.class) && !ZValidator.isZMinZMaxSupported(f.getType())) {
						ZValidator.throwTypeNotSupportedExcpetion(cls, ZPositive.class, f);
					} else if (annotation.annotationType() == ZCustom.class) {

						if (!ZValidator.isZCustomSupported(f.getType())) {
							ZValidator.throwTypeNotSupportedExcpetion(cls, ZCustom.class, f);
						}

						final Class<? extends ZCustomValidator> customClass = f.getAnnotation(ZCustom.class).cls();
						try {
							ZSingleton.getSingletonByClass(customClass);
						} catch (final Exception e) {
							final String message = Task.gExceptionMessage(e);
							throw new ValidatedException("@" + ZCustom.class.getSimpleName() + ".cls 指定的类型["
									+ customClass + "]初始化异常,message=" + message);
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

	private final static HashSet<Class<? extends Annotation>> VA_SET = Sets.newHashSet(ZNotNull.class, ZNotEmtpy.class,
			ZStartWith.class, ZEndsWith.class, ZLength.class, ZMin.class, ZMax.class, ZPositive.class);

	public static boolean isValidatorAnnotation(final Class<? extends Annotation> annoClass) {
		return ZValidator.VA_SET.contains(annoClass);
	}

}
