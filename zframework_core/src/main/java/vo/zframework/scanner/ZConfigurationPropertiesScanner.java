package vo.zframework.scanner;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import vo.zframework.anno.ZAutowired;
import vo.zframework.anno.ZConfigurationProperties;
import vo.zframework.anno.ZConfigurationPropertiesRegistry;
import vo.zframework.anno.ZOrder;
import vo.zframework.anno.ZOrderComparator;
import vo.zframework.anno.ZValue;
import vo.zframework.cache.AU;
import vo.zframework.cache.CU;
import vo.zframework.cache.STU;
import vo.zframework.configuration.ZProperties;
import vo.zframework.core.RU;
import vo.zframework.core.ZContext;
import vo.zframework.core.ZSingleton;
import vo.zframework.exception.StartupException;
import vo.zframework.exception.TypeNotSupportedExcpetion;
import vo.zframework.validator.ZConfigurationPropertiesException;
import vo.zframework.validator.ZValidator;

/**
 * 扫描 @ZConfigurationProperties 的类，从配置文件读取配置组长一个此类的对象
 *
 * @author zhangzhen
 * @date 2023年6月29日
 *
 */
public class ZConfigurationPropertiesScanner {

	/**
	 * 	List和Set中根据[i]取值的最大值，从[0]开始：
	 *  0 1 2 3...最大支持到此值
	 */
	public static final int PROPERTY_INDEX = 1520;

	/**
	 * List的泛型参数对象里支持的字段类型
	 */
	private static final List<Class<?>> LT = List.of(Byte.class, Short.class,
			Integer.class, Long.class, Float.class, Double.class, Character.class, Boolean.class, String.class);

	public static void scanAndCreate(final String... packageName) throws Exception {

		final Set<Class<?>> csSet = ClassMap.scanPackageByAnnotation(ZConfigurationProperties.class, packageName);
		if (CU.isEmpty(csSet)) {
			return;
		}

		final Set<Integer> valueSet = new HashSet<>();
		for (final Class<?> cls : csSet) {
			final ZOrder annotation = cls.getAnnotation(ZOrder.class);
			if ((annotation != null) && !valueSet.add(annotation.value())) {
				throw new StartupException("@" + ZConfigurationProperties.class.getSimpleName() + " 类 " + "@"
						+ ZOrder.class.getSimpleName() + ".value" + "[" + annotation.value() + "]" + "重复，请检查代码");
			}
		}

		final List<Class<?>> cl = new ArrayList<>(csSet);
		cl.sort(new ZOrderComparator<>());

		for (final Class<?> cs : cl) {

			 final ZConfigurationProperties zcp = cs.getAnnotation(ZConfigurationProperties.class);

			 final String prefix = STU.isEmpty(zcp.prefix()) ? ""
					 : zcp.prefix().endsWith(".") ? zcp.prefix() : zcp.prefix() + ".";

			 final Object object = ZSingleton.getSingletonByClass(cs);
			 final Field[] fs = cs.getDeclaredFields();

			 for (final Field field : fs) {

				 checkModifiers(cs, field);
				 try {
					 findValueAndSetValue(prefix, object, field);
				 } catch (final Exception e) {
					 throw e;
				 }
				 // 赋值后校验一下
				 ZValidator.validatedAll(object, field);
			}

			ZContext.addBean(cs, object);
		}

		for (final Class<?> cls : csSet) {
			final Field[] declaredFields = cls.getDeclaredFields();

			// 如果Class有 @ZAutowired 字段，则先生成对应的的对象，然后注入进来
			Arrays.stream(declaredFields)
			.filter(f -> f.isAnnotationPresent(ZAutowired.class))
			.forEach(f -> ZAutowiredScanner.inject(cls, f));

			// 如果Class有 @ZValue 字段 ，则先给此字段注入值
			Arrays.stream(cls.getDeclaredFields())
			.filter(f -> f.isAnnotationPresent(ZValue.class))
			.forEach(f -> ZValueScanner.inject(cls, f));
		}

		final List<Object> zcpList = ZContext.all().values().stream()
				.filter(b -> b.getClass().isAnnotationPresent(ZConfigurationProperties.class))
				.collect(Collectors.toList());
		final ZConfigurationPropertiesRegistry configurationPropertiesRegistry = ZSingleton.getSingletonByClass(ZConfigurationPropertiesRegistry.class);
		for (final Object zcp : zcpList) {
			configurationPropertiesRegistry.addConfigurationPropertie(zcp);
		}
		ZContext.addBean(ZConfigurationPropertiesRegistry.class, configurationPropertiesRegistry);

	}

	private static void findValueAndSetValue(final String prefix, final Object object, final Field field) throws Exception {
		final Class<?> type = field.getType();

		final AtomicReference<String> keyAR = new AtomicReference<>();
		final String key = prefix + field.getName();
		if (ZProperties.containsKey(key)) {
			keyAR.set(key);

			setValueByType(object, field, type, keyAR);

			return;
		}

		final String convert = convert(key);
		keyAR.set(convert);

		// 无 java 字段直接对应的 配置项,则 把[orderCount]转为[order.count]再试，如果包含了则继续赋值
		if (ZProperties.containsKey(convert)) {
			setValueByType(object, field, type, keyAR);
		} else {
			// 到此 [orderCount]和[order.count]形式的名称都不匹配，说明是List、Map、Set三种类型了，开始匹配这三种类型
			if (field.getType() == Map.class) {
				setMap(object, field, key);
			} else if (field.getType() == List.class) {
				// FIXME 2023年11月9日 上午12:13:59 zhanghen: 支持三种类型要支持什么类型
				checkList(field);
				setList(object, field, key);
			} else if (field.getType() == Set.class) {
				setSet(object, field, key);
			}

		}

	}

	private static void checkList(final Field field) {
		final Class<?>[] ts = ZCU.getGenericType(field);
		if (AU.isEmpty(ts)) {
			final String message = "@" + ZConfigurationProperties.class.getSimpleName() + " List类型必须加入泛型参数";
			throw new ZConfigurationPropertiesException(message);
		}

		final boolean isJavaType = LT.contains(ts[0]);
		final boolean isUserType = !isJavaType
				&& !ts[0].getCanonicalName().startsWith("java");
		if (!isUserType) {
			final String message = "@" + ZConfigurationProperties.class.getSimpleName() + " List类型只支持用户自定义类型"
					+ ","
					+ "当前类型=" + ts[0].getCanonicalName()
					;
			throw new ZConfigurationPropertiesException(message);
		}

		for (final Field f : ts[0].getDeclaredFields()) {
			if (!LT.contains(f.getType())) {
				throw new StartupException(
						ts[0].getClass().getSimpleName() + "] 中的字段[" + f.getType().getCanonicalName() + " "
								+ f.getName() + "]类型不支持,支持字段类型为" + LT);
			}
		}
	}

	private static void setSet(final Object object, final Field field, final String key) {

		final Class<?>[] ts = ZCU.getGenericType(field);
		if (AU.isEmpty(ts)) {
			final String message = object.getClass().getSimpleName() + "." + field.getName() + " Set类型必须加入泛型参数" ;
			throw new ZConfigurationPropertiesException(message);
		}

		final Set<Object> vs = Collections.synchronizedSet(new LinkedHashSet<>());

		IntStream.range(1, PROPERTY_INDEX + 1)
		.parallel()
		.forEach(i -> {
			final String suffix = "[" + (i - 1) + "]";

			final String k1 = key + suffix;
			final String v1 = ZProperties.getString(k1);
			if (STU.isNotEmpty(v1) && !vs.add(v1)) {
				th(object, field, k1, v1);
			}

			final String k2 = convert(key) + suffix;
			final String v2 = ZProperties.getString(k2);
			if (STU.isNotEmpty(v2) && !vs.add(v2)) {
				th(object, field, k2, v2);
			}

		});

		if (!vs.isEmpty()) {
			RU.setFiledValue(field, object, vs);
		}
	}

	private static void th(final Object object, final Field field, final String key, final String value) {
		final String message = object.getClass().getSimpleName() + "." + field.getName() + " Set类型值重复：key=" + key
				+ ",value=" + value;
		throw new ZConfigurationPropertiesException(message);
	}

	private static void setList(final Object object, final Field field, final String key) throws Exception {

		// 从1-N个[i]
		final List<Object> list = new ArrayList<>();

		for (int i = 1; i <= (PROPERTY_INDEX + 1); i++) {

			final Object newInstance = newInstance(field);

			final String suffix = "[" + (i - 1) + "]";
			final String fullKey1 = key + suffix;
			final Iterator<String> sk1 = ZProperties.getKeys(fullKey1);

			boolean sk1HasNext = false;

			if (sk1.hasNext()) {
				sk1HasNext = true;
				iteratorList(field, list, fullKey1, sk1, newInstance);
			}

			final String fullKey2 = convert(key) + suffix;
			if (!Objects.equals(fullKey1, fullKey2)) {

				final Iterator<String> sk2 = ZProperties.getKeys(fullKey2);

				if (sk2.hasNext()) {
					if (sk1HasNext) {
						final String message = "@" + ZConfigurationProperties.class.getSimpleName()
								+ " List类型参数初始化异常，key：" + fullKey1 + " 和 " + fullKey2 + " 配置重复，请只使用其中一种方式，建议使用 "
								+ fullKey2 + " 的形式";
						throw new ZConfigurationPropertiesException(message);
					}
					sk1HasNext = true;
					iteratorList(field, list, fullKey2, sk2, newInstance);
				}
			}

			if (sk1HasNext) {
				list.add(newInstance);
			}

			// FIXME 2024年2月16日 下午7:35:20 zhanghen: 下面的之前考虑的可以 0配置 1配置null 2配置
			// 形成 List list = {配置1,null,配置2}的这种形式还待考虑怎么实现好，或者不做这个功能了
			//			else {
			//				// 为空也add null，占一个位置，为了这种需求：
			//				// [0]=A [2]=C 就是不配置第二个位置让其为空，
			//				// 这样取的时候list.get(1) 取得的第二个就是null
			//
			////				list.add(null);
			//			}

			//			list.add(newInstance);
		}


		if (list.isEmpty()) {
			return;
		}

		// 最后去除后面的所有的null
		int i = list.size();
		//		int i = list.size() - 1;
		while (i > 1) {
			if (list.get(i - 1) != null) {
				break;
			}
			i--;
		}

		final List<Object> subList = i <= 0 ? null : list.subList(0, i);

		if (CU.isNotEmpty(subList)) {
			RU.setFiledValue(field, object, subList);
		}
	}

	private static Object iteratorList(final Field field, final List<Object> list, final String fullKey1,
			final Iterator<String> sk1, final Object newInstance) throws Exception {
		final String xa = sk1.next();
		final String xaValue = ZProperties.getString(xa);
		//		final Object newInstance = newInstance(field);
		try {
			setValue(newInstance, fullKey1, xa, xaValue);
		} catch (final Exception e) {
			final String message = "@" + ZConfigurationProperties.class.getSimpleName()
					+ " List类型参数初始化异常，key=" + xa;
			throw new ZConfigurationPropertiesException(message);
		}

		while (sk1.hasNext()) {
			final String xa2 = sk1.next();
			final String xaValue2 = ZProperties.getString(xa2);
			if (xaValue2 != null) {
				setValue(newInstance, fullKey1, xa2, xaValue2);
			}
		}
		//		list.add(newInstance);
		return newInstance;
	}

	private static void setValue(final Object newInstance, final String fullKey, final String key,
			final String value) throws Exception {
		final String fieldName = key.replace(fullKey + ".", "");

		final Field field = RU.getDeclaredField(newInstance, fieldName);

		final Class<?> ft = field.getType();

		if ((ft == byte.class) || (ft == Byte.class)) {
			RU.setFiledValue(field, newInstance, Byte.parseByte(value));
		} else if ((ft == short.class) || (ft == Short.class)) {
			RU.setFiledValue(field, newInstance, Short.parseShort(value));
		} else if ((ft == int.class) || (ft == Integer.class)) {
			RU.setFiledValue(field, newInstance, Integer.parseInt(value));
		} else if ((ft == long.class) || (ft == Long.class)) {
			RU.setFiledValue(field, newInstance, Long.parseLong(value));
		} else if ((ft == float.class) || (ft == Float.class)) {
			RU.setFiledValue(field, newInstance, Float.parseFloat(value));
		} else if ((ft == double.class) || (ft == Double.class)) {
			RU.setFiledValue(field, newInstance, Double.parseDouble(value));
		} else if ((ft == boolean.class) || (ft == Boolean.class)) {
			// FIXME 2023年11月9日 下午1:53:34 zhanghen: TODO 其他类型继续提示
			final String bo = String.valueOf(value);
			if (!"true".equalsIgnoreCase(bo) && !"false".equalsIgnoreCase(bo)) {
				// Boolean.parseBoolean 也无需校验，但仍提示
				throw new ConfigurationPropertiesParameterException(
						newInstance.getClass().getSimpleName() + "." + fieldName + " 为 "
								+ Boolean.class.getSimpleName() + " 类型，当前参数为 " + value + "，请检查代码参数类型或修改参数值为true或false");
			}
			RU.setFiledValue(field, newInstance, Boolean.parseBoolean(value));
		} else if ((ft == char.class) || (ft == Character.class)) {
			if (value.length() > 1) {
				throw new ConfigurationPropertiesParameterException(
						newInstance.getClass().getSimpleName() + "." + fieldName + " 为 "
								+ Character.class.getSimpleName() + " 类型，当前参数为 " + value + "，请检查代码参数类型或修改参数值为一个字符");
			}
			RU.setFiledValue(field, newInstance, Character.valueOf(value.charAt(0)));
		} else if (ft == String.class) {
			// String 无需校验直接赋值
			RU.setFiledValue(field, newInstance, value);
		} else {
			throw new TypeNotSupportedExcpetion(fieldName);
		}

	}

	private static Object newInstance(final Field field)  {
		final Class<?> type = ZCU.getGenericType(field)[0];
		Object newInstance = null;
		try {
			newInstance = type.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException e) {
			e.printStackTrace();
		}

		return newInstance;
	}

	private static void setMap(final Object object, final Field field, final String key) {
		final Map<String, Object> map = new HashMap<>(8, 1F);
		final Iterator<String> keys = ZProperties.getKeys(key);
		while (keys.hasNext()) {
			final String k = keys.next();

			final String kName = k.replace(key + '.', STU.EMPTY);

			final String value = ZProperties.getString(k);
			map.put(kName, value);
		}

		final String convert = convert(key);
		final Iterator<String> keys2 = ZProperties.getKeys(convert);
		while (keys2.hasNext()) {
			final String k = keys2.next();

			final String kName = k.replace(convert + '.', STU.EMPTY);

			final String value = ZProperties.getString(k);
			map.put(kName, value);
		}

		if (!map.isEmpty()) {
			RU.setFiledValue(field, object, map);
		}
	}

	private static void setValueByType(final Object object, final Field field, final Class<?> type,
			final AtomicReference<String> keyAR) {

		if (type == String.class) {
			final String v1 = getStringValue(keyAR);
			RU.setFiledValue(field, object, v1);
		} else if ((type == byte.class) || (type == Byte.class)) {
			RU.setFiledValue(field, object, ZProperties.getByte(keyAR.get()));
		} else if ((type == short.class) || (type == Short.class)) {
			RU.setFiledValue(field, object, ZProperties.getShort(keyAR.get()));
		} else if ((type == int.class) || (type == Integer.class)) {
			RU.setFiledValue(field, object, ZProperties.getInteger(keyAR.get()));
		} else if ((type == long.class) || (type == Long.class)) {
			RU.setFiledValue(field, object, ZProperties.getLong(keyAR.get()));
		} else if ((type == float.class) || (type == Float.class)) {
			RU.setFiledValue(field, object, ZProperties.getFloat(keyAR.get()));
		} else if ((type == double.class) || (type == Double.class)) {
			RU.setFiledValue(field, object, ZProperties.getDouble(keyAR.get()));
		} else if ((type == char.class) || (type == Character.class)) {
			final String v1 = getStringValue(keyAR);
			RU.setFiledValue(field, object, v1.charAt(0));
		} else if ((type == boolean.class) || (type == Boolean.class)) {
			RU.setFiledValue(field, object, ZProperties.getBoolean(keyAR.get()));
		} else if (type == BigInteger.class) {
			RU.setFiledValue(field, object, ZProperties.getBigInteger(keyAR.get()));
		} else if (type == BigDecimal.class) {
			RU.setFiledValue(field, object, ZProperties.getBigDecimal(keyAR.get()));
		} else if (type == AtomicInteger.class) {
			RU.setFiledValue(field, object, new AtomicInteger(ZProperties.getInteger(keyAR.get())));
		} else if (type == AtomicLong.class) {
			RU.setFiledValue(field, object, new AtomicLong(ZProperties.getLong(keyAR.get())));
		}

	}

	private static String getStringValue(final AtomicReference<String> keyAR) {
		final StringJoiner joiner = new StringJoiner(",");
		try {
			final String[] stringArray = ZProperties.getStringArray(keyAR.get());
			for (final String s : stringArray) {
				final String s2 = new String(s.trim()
						.getBytes(),
						Charset.defaultCharset().displayName());
				joiner.add(s2);
			}
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return joiner.toString();
	}

	private static void checkModifiers(final Class<?> cs, final Field field) {
		// FIXME 2023年11月28日 下午5:31:44 zhanghen: XXX 下面的校验似乎不校验也可以？
		//		final int modifiers = field.getModifiers();
		//		if (Modifier.isPublic(modifiers)) {
		//			throw new IllegalArgumentException("@" + ZConfigurationProperties.class.getSimpleName() + " 类 "
		//					+ cs.getSimpleName() + " 的字段 " + field.getName() + " 不能用public修饰");
		//		}
		//		if (Modifier.isStatic(modifiers)) {
		//			throw new IllegalArgumentException("@" + ZConfigurationProperties.class.getSimpleName() + " 类 "
		//					+ cs.getSimpleName() + " 的字段 " + field.getName() + " 不能用static修饰");
		//		}
		//		if (Modifier.isFinal(modifiers)) {
		//			throw new IllegalArgumentException("@" + ZConfigurationProperties.class.getSimpleName() + " 类 "
		//					+ cs.getSimpleName() + " 的字段 " + field.getName() + " 不能用final修饰");
		//		}
		//		if (Modifier.isAbstract(modifiers)) {
		//			throw new IllegalArgumentException("@" + ZConfigurationProperties.class.getSimpleName() + " 类 "
		//					+ cs.getSimpleName() + " 的字段 " + field.getName() + " 不能用abstract修饰");
		//		}
	}

	/**
	 * 从 orderCount 形式的字段名称， 获取 order.count 形式的名称，
	 * 把其中的[大写字母]替换为[.小写字母]
	 *
	 * @param fieldName
	 * @return
	 *
	 */
	public static String convert(final String fieldName) {
		final StringBuilder builder = new StringBuilder(fieldName);
		final char[] ca = fieldName.toCharArray();
		final AtomicInteger replaceCount = new AtomicInteger(0);
		for (int i = 0; i < ca.length; i++) {
			final char c = ca[i];
			if (daxie.contains(c)) {
				final int andIncrement = replaceCount.getAndIncrement();
				builder.replace(i + andIncrement, i + andIncrement + 1, "." + Character.toLowerCase(c));
			}
		}
		return builder.toString();
	}

	static Set<Character> daxie = null;

	static {
		final HashSet<Character> hashSet = new HashSet<>();

		Collections.addAll(hashSet, 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q',
				'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z');

		daxie = hashSet;
	}


}
