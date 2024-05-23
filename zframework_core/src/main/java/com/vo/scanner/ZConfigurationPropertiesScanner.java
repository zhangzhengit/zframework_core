package com.vo.scanner;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.configuration.PropertiesConfiguration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vo.anno.ZAutowired;
import com.vo.anno.ZConfigurationProperties;
import com.vo.anno.ZConfigurationPropertiesRegistry;
import com.vo.anno.ZOrder;
import com.vo.anno.ZOrderComparator;
import com.vo.anno.ZValue;
import com.vo.configuration.ZProperties;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;
import com.vo.core.ZSingleton;
import com.vo.exception.StartupException;
import com.vo.exception.TypeNotSupportedExcpetion;
import com.vo.validator.ZConfigurationPropertiesException;
import com.vo.validator.ZValidator;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;

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
	private static final ZLog2 LOG = ZLog2.getInstance();

	public static void scanAndCreate(final String... packageName) throws Exception {

		final Set<Class<?>> csSet = scanPackage(packageName).stream()
				.filter(cls -> cls.isAnnotationPresent(ZConfigurationProperties.class))
				.collect(Collectors.toSet());
		if (CollUtil.isEmpty(csSet)) {
			return;
		}

		final ArrayList<Class<?>> cl = Lists.newArrayList(csSet);
		final Set<Integer> valueSet = Sets.newHashSet();
		for (final Class<?> cls : cl) {
			final ZOrder annotation = cls.getAnnotation(ZOrder.class);
			if (annotation != null && !valueSet.add(annotation.value())) {
				throw new StartupException("@" + ZConfigurationProperties.class.getSimpleName() + " 类 " + "@"
						+ ZOrder.class.getSimpleName() + ".value" + "[" + annotation.value() + "]" + "重复，请检查代码");
			}
		}

		final List<Class<?>> sl = cl.stream().sorted(new ZOrderComparator<>()).collect(Collectors.toList());

		for (final Class<?> cs : sl) {

			final ZConfigurationProperties zcp = cs.getAnnotation(ZConfigurationProperties.class);

			final String prefix = StrUtil.isEmpty(zcp.prefix()) ? ""
					: zcp.prefix().endsWith(".") ? zcp.prefix() : zcp.prefix() + ".";

			final Object object = ZSingleton.getSingletonByClass(cs);
			final Field[] fs = cs.getDeclaredFields();
			for (final Field field : fs) {
				checkModifiers(cs, field);
				findValueAndSetValue(prefix, object, field);
			}

			System.out.println("ZCP-object = " + object);

			ZContext.addBean(cs, object);
			ZContext.addBean(cs.getCanonicalName(), object);
		}


		for (final Class<?> cls : csSet) {
			// 如果Class有 @ZAutowired 字段，则先生成对应的的对象，然后注入进来
			Lists.newArrayList(cls.getDeclaredFields()).stream()
				.filter(f -> f.isAnnotationPresent(ZAutowired.class))
				.forEach(f -> ZAutowiredScanner.inject(cls, f));

			// 如果Class有 @ZValue 字段 ，则先给此字段注入值
			Lists.newArrayList(cls.getDeclaredFields()).stream()
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
		final PropertiesConfiguration p = ZProperties.getInstance();
		final Class<?> type = field.getType();

		final AtomicReference<String> keyAR = new AtomicReference<>();
		final String key = prefix + field.getName();
		if (p.containsKey(key)) {
			keyAR.set(key);

			setValueByType(object, field, p, type, keyAR);

			return;
		}

		final String convert = convert(key);
		keyAR.set(convert);

		// 无 java 字段直接对应的 配置项,则 把[orderCount]转为[order.count]再试，如果包含了则继续赋值
		if (p.containsKey(convert)) {
			setValueByType(object, field, p, type, keyAR);
		} else {
			// 到此 [orderCount]和[order.count]形式的名称都不匹配，说明是List、Map、Set三种类型了，开始匹配这三种类型

			if (field.getType().getCanonicalName().equals(Map.class.getCanonicalName())) {
				setMap(object, field, p, key);
			} else if (field.getType().getCanonicalName().equals(List.class.getCanonicalName())) {
				// FIXME 2023年11月9日 上午12:13:59 zhanghen: 支持三种类型要支持什么类型

				final Class<?>[] ts = ZCU.getGenericType(field);
				if (ArrayUtil.isEmpty(ts)) {
					final String message = "@" + ZConfigurationProperties.class.getSimpleName() + " List类型必须加入泛型参数";
					throw new ZConfigurationPropertiesException(message);
				}

				final boolean isJavaType = LIST_T_FIELD_TYPE.contains(ts[0].getCanonicalName());
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
					if (!LIST_T_FIELD_TYPE.contains(f.getType().getCanonicalName())) {
						throw new StartupException(
								ts[0].getClass().getSimpleName() + "] 中的字段[" + f.getType().getCanonicalName() + " "
										+ f.getName() + "]类型不支持,支持字段类型为" + LIST_T_FIELD_TYPE);
					}
				}

				setList(object, field, p, key);
			} else if (field.getType().getCanonicalName().equals(Set.class.getCanonicalName())) {
				setSet(object, field, p, key);
			}

			ZValidator.validatedAll(object, field);

		}

	}

	/**
	 * 	List的泛型参数对象里支持的字段类型
	 */
	public static final
		ImmutableList<String> LIST_T_FIELD_TYPE = ImmutableList.copyOf(
					Lists.newArrayList(
								Byte.class.getCanonicalName(),
								Short.class.getCanonicalName(),
								Integer.class.getCanonicalName(),
								Long.class.getCanonicalName(),
								Float.class.getCanonicalName(),
								Double.class.getCanonicalName(),
								Character.class.getCanonicalName(),
								Boolean.class.getCanonicalName(),
								String.class.getCanonicalName()
							)

				);



	private static void setSet(final Object object, final Field field, final PropertiesConfiguration p,
			final String key) {

		// 从1-N个[i]
		final Set<Object> set = Sets.newLinkedHashSet();

		final Class<?>[] ts = ZCU.getGenericType(field);
		if (ArrayUtil.isEmpty(ts)) {
			final String message = object.getClass().getSimpleName() + "." + field.getName() + " Set类型必须加入泛型参数" ;
			throw new ZConfigurationPropertiesException(message);
		}

		for (int i = 1; i <= PROPERTY_INDEX + 1; i++) {
			final String suffix = "[" + (i - 1) + "]";
			final String k1 = key + suffix;

			if (p.containsKey(k1)) {
				iteratorSet(object, field, p, set, ts, k1);
			} else {

				final String k2 = convert(key) + suffix;
				if (p.containsKey(k2)) {
					iteratorSet(object, field, p, set, ts, k2);
				}
			}
		}
		System.out.println("set = " + set);
		try {
			if (!set.isEmpty()) {
				field.setAccessible(true);
				field.set(object, set);
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private static void iteratorSet(final Object object, final Field field, final PropertiesConfiguration p,
			final Set<Object> set, final Class<?>[] ts, final String k1) {
		final Iterator<String> sk = p.getKeys(k1);
		while (sk.hasNext()) {
			final String xa = sk.next();

			final Class<?> gType = ts[0];
			final Object value = getSetFiledValue(p, xa, gType);

			if (!set.add(value)) {
				final String message = object.getClass().getSimpleName() + "." + field.getName() + " Set类型值重复：key="
						+ xa + "" + ",value=" + value;
				throw new ZConfigurationPropertiesException(message);
			}
		}
	}

	private static Object getSetFiledValue(final PropertiesConfiguration p, final String xa, final Class<?> gType) {
		Object value = null;
		if (gType.equals(String.class)) {
			value = p.getString(xa);
		} else if (gType.equals(Byte.class)) {
			value = p.getByte(xa);
		} else if (gType.equals(Short.class)) {
			value = p.getShort(xa);
		} else if (gType.equals(Integer.class)) {
			value = p.getInteger(xa, null);
		} else if (gType.equals(Long.class)) {
			value = p.getLong(xa);
		} else if (gType.equals(Float.class)) {
			value = p.getFloat(xa);
		} else if (gType.equals(Double.class)) {
			value = p.getDouble(xa);
		} else if (gType.equals(Character.class)) {
			value = StrUtil.isEmpty(p.getString(xa)) ? null : p.getString(xa).charAt(0);
		} else if (gType.equals(Boolean.class)) {
			value = p.getBoolean(xa);
		} else {
			final String message = "@" + ZConfigurationProperties.class.getSimpleName() + " Set类型的泛型参数不支持,type = "
					+ gType.getCanonicalName();
			throw new ZConfigurationPropertiesException(message);
		}
		return value;
	}

	private static void setList(final Object object, final Field field, final PropertiesConfiguration p,
			final String key) throws Exception {

		// 从1-N个[i]
		final List<Object> list = Lists.newArrayList();

		for (int i = 1; i <= PROPERTY_INDEX + 1; i++) {

			final Object newInstance = newInstance(field);

			final String suffix = "[" + (i - 1) + "]";
			final String fullKey1 = key + suffix;
			final Iterator<String> sk1 = p.getKeys(fullKey1);


			boolean sk1HasNext = false;

			if (sk1.hasNext()) {
				sk1HasNext = true;
				iteratorList(field, p, list, fullKey1, sk1, newInstance);
			}

			final String fullKey2 = convert(key) + suffix;
			if (!Objects.equals(fullKey1, fullKey2)) {

				final Iterator<String> sk2 = p.getKeys(fullKey2);

				if (sk2.hasNext()) {
					if (sk1HasNext) {
						final String message = "@" + ZConfigurationProperties.class.getSimpleName()
								+ " List类型参数初始化异常，key：" + fullKey1 + " 和 " + fullKey2 + " 配置重复，请只使用其中一种方式，建议使用 "
								+ fullKey2 + " 的形式";
						throw new ZConfigurationPropertiesException(message);
					}
					sk1HasNext = true;
					iteratorList(field, p, list, fullKey2, sk2, newInstance);
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

		System.out.println("list = " + subList);

		try {
			if (CollUtil.isNotEmpty(subList)) {
				field.setAccessible(true);
				field.set(object, subList);
			}

		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private static Object iteratorList(final Field field, final PropertiesConfiguration p, final List<Object> list,
			final String fullKey1, final Iterator<String> sk1, final Object newInstance) throws Exception {
		final String xa = sk1.next();
		final String xaValue = p.getString(xa);
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
			final String xaValue2 = p.getString(xa2);
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

		try {
			final Field field = newInstance.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);

			if (field.getType().getCanonicalName().equals(Byte.class.getCanonicalName())) {
				field.set(newInstance, Byte.parseByte(value));
			} else if (field.getType().getCanonicalName().equals(Short.class.getCanonicalName())) {
				field.set(newInstance, Short.parseShort(value));
			} else if (field.getType().getCanonicalName().equals(Integer.class.getCanonicalName())) {
				field.set(newInstance, Integer.parseInt(value));
			} else if (field.getType().getCanonicalName().equals(Long.class.getCanonicalName())) {
				field.set(newInstance, Long.parseLong(value));
			} else if (field.getType().getCanonicalName().equals(Float.class.getCanonicalName())) {
				field.set(newInstance, Float.parseFloat(value));
			} else if (field.getType().getCanonicalName().equals(Double.class.getCanonicalName())) {
				field.set(newInstance, Double.parseDouble(value));
			} else if (field.getType().getCanonicalName().equals(Boolean.class.getCanonicalName())) {
				// FIXME 2023年11月9日 下午1:53:34 zhanghen: TODO 其他类型继续提示
				final String bo = String.valueOf(value);
				if (!"true".equalsIgnoreCase(bo) && !"false".equalsIgnoreCase(bo)) {
					// Boolean.parseBoolean 也无需校验，但仍提示
					throw new ConfigurationPropertiesParameterException(
							newInstance.getClass().getSimpleName() + "." + fieldName + " 为 "
									+ Boolean.class.getSimpleName() + " 类型，当前参数为 " + value + "，请检查代码参数类型或修改参数值为true或false");
				}
				field.set(newInstance, Boolean.parseBoolean(value));
			} else if (field.getType().getCanonicalName().equals(Character.class.getCanonicalName())) {
				if (value.length() > 1) {
					throw new ConfigurationPropertiesParameterException(
							newInstance.getClass().getSimpleName() + "." + fieldName + " 为 "
									+ Character.class.getSimpleName() + " 类型，当前参数为 " + value + "，请检查代码参数类型或修改参数值为一个字符");
				}
				field.set(newInstance, Character.valueOf(value.charAt(0)));
			} else if (field.getType().getCanonicalName().equals(String.class.getCanonicalName())) {
				// String 无需校验直接赋值
				field.set(newInstance, value);
			} else {
				throw new TypeNotSupportedExcpetion(fieldName);
			}

		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			throw e;
		}
	}

	private static Object newInstance(final Field field)  {
		final Class<?> type2 = ZCU.getGenericType(field)[0];
		Object newInstance = null;
		try {
			newInstance = type2.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}

		return newInstance;
	}

	private static void setMap(final Object object, final Field field, final PropertiesConfiguration p,
			final String key) {
		final Iterator<String> keys = p.getKeys(key);
		final Map<String, Object> map = new HashMap<>(16, 1F);
		while (keys.hasNext()) {
			final String k = keys.next();

			final String kName = StrUtil.removeAll(k, key + ".");

			final String value = p.getString(k);
			map.put(kName, value);
		}

		try {
			if (!map.isEmpty()) {
				field.setAccessible(true);
				field.set(object, map);
			}

		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private static void setValueByType(final Object object, final Field field, final PropertiesConfiguration p,
			final Class<?> type, final AtomicReference<String> keyAR) {

		final String v1 = getStringValue(p, keyAR);


		if (type.getCanonicalName().equals(String.class.getCanonicalName())) {
			setValue(object, field, v1);
		} else if (type.getCanonicalName().equals(Byte.class.getCanonicalName())) {
			setValue(object, field, p.getByte(keyAR.get()));
		} else if (type.getCanonicalName().equals(Short.class.getCanonicalName())) {
			setValue(object, field, p.getShort(keyAR.get()));
		} else if (type.getCanonicalName().equals(Integer.class.getCanonicalName())) {
			setValue(object, field, p.getInt(keyAR.get()));
		} else if (type.getCanonicalName().equals(Long.class.getCanonicalName())) {
			setValue(object, field, p.getLong(keyAR.get()));
		} else if (type.getCanonicalName().equals(Float.class.getCanonicalName())) {
			setValue(object, field, p.getFloat(keyAR.get()));
		} else if (type.getCanonicalName().equals(Double.class.getCanonicalName())) {
			setValue(object, field, p.getDouble(keyAR.get()));
		} else if (type.getCanonicalName().equals(Character.class.getCanonicalName())) {
			setValue(object, field, v1.charAt(0));
		} else if (type.getCanonicalName().equals(Boolean.class.getCanonicalName())) {
			setValue(object, field, p.getBoolean(keyAR.get()));
		} else if (type.getCanonicalName().equals(BigInteger.class.getCanonicalName())) {
			setValue(object, field, p.getBigInteger(keyAR.get()));
		} else if (type.getCanonicalName().equals(BigDecimal.class.getCanonicalName())) {
			setValue(object, field, p.getBigDecimal(keyAR.get()));
		} else if (type.getCanonicalName().equals(AtomicInteger.class.getCanonicalName())) {
			setValue(object, field, new AtomicInteger(p.getInt(keyAR.get())));
		} else if (type.getCanonicalName().equals(AtomicLong.class.getCanonicalName())) {
			setValue(object, field, new AtomicLong(p.getInt(keyAR.get())));
		}

		// 赋值以后才可以校验
		ZValidator.validatedAll(object, field);
	}

	private static String getStringValue(final PropertiesConfiguration p, final AtomicReference<String> keyAR) {
		final StringJoiner joiner = new StringJoiner(",");
		try {
			final String[] stringArray = p.getStringArray(keyAR.get());
			for (final String s : stringArray) {
				final String s2 = new String(s.trim()
						.getBytes(ZProperties.PROPERTIESCONFIGURATION_ENCODING.get()),
						Charset.defaultCharset().displayName());
				joiner.add(s2);
			}
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return joiner.toString();
	}

	@SuppressWarnings("boxing")




	private static void setValue(final Object object, final Field field, final Object value) {
		try {
			field.setAccessible(true);
			field.set(object, value);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
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

	public static Set<Class<?>> scanPackage(final String... packageName) {
		LOG.info("开始扫描类,scanPackage={}", Arrays.toString(packageName));
		final HashSet<Class<?>> rs = Sets.newHashSet();
		for (final String p : packageName) {
			final Set<Class<?>> clsSet = ClassMap.scanPackage(p);
			rs.addAll(clsSet);

		}
		return rs;
	}

	/**
	 * 从 orderCount 形式的字段名称， 获取 order.count 形式的名称，
	 * 把其中的[大写字母]替换为[.小写字母]
	 *
	 * @param fieldName
	 * @return
	 *
	 */
	private static String convert(final String fieldName) {
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

	static HashSet<Character> daxie = Sets.newHashSet('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
			'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z');



}
