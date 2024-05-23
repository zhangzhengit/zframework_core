package com.vo.scanner;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.configuration.PropertiesConfiguration;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vo.anno.ZComponent;
import com.vo.anno.ZController;
import com.vo.anno.ZService;
import com.vo.anno.ZValue;
import com.vo.configuration.ZProperties;
import com.vo.core.Task;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;
import com.vo.validator.ZValidator;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReflectUtil;

/**
 *
 * 扫描组件中 带有 @ZValue 的字段，根据name注入配置文件中对应的value
 *
 * @author zhangzhen
 * @date 2023年6月29日
 *
 */
public class ZValueScanner {

	/**
	 * <@ZValue.listenForChanges = true的字段，此字段所在的对象>
	 */
	private final static ConcurrentMap<Field, Object> valueMap = Maps.newConcurrentMap();
	private final static HashBasedTable<String, Field, Object> valueTable = HashBasedTable.create();

	public static void inject(final String... packageName) {
		final Set<Class<?>> zcSet = ClassMap.scanPackageByAnnotation(ZComponent.class, packageName);
		final Set<Class<?>> zc2Set = ClassMap.scanPackageByAnnotation(ZController.class, packageName);
		final Set<Class<?>> zc3Set = ClassMap.scanPackageByAnnotation(ZService.class, packageName);

		final List<Class<?>> clist = Lists.newArrayListWithCapacity(zcSet.size() + zc2Set.size());
		clist.addAll(zcSet);
		clist.addAll(zc2Set);
		clist.addAll(zc3Set);

		if (clist.isEmpty()) {
			return;
		}

		for (final Class<?> cls : clist) {
			final Object bean = ZContext.getBean(cls.getCanonicalName());
			if (Objects.isNull(bean)) {
				continue;
			}

			final Field[] fields = ReflectUtil.getFields(bean.getClass());
			for (final Field field : fields) {
				inject(cls, field);
			}
		}
	}

	public static void inject(final Class<?> cls, final Field field) {
		final Object bean = ZContext.getBean(cls.getCanonicalName());
		final ZValue value = field.getAnnotation(ZValue.class);
		if (value == null) {
			return;
		}

		if (value.listenForChanges()) {
			valueMap.put(field, bean);
			valueTable.put(value.name(), field, bean);
		}

		setValue(field, value, bean);
	}

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static void updateValue(final String name, final Object value)  {
		final Map<Field, Object> map = valueTable.row(name);
		if (CollUtil.isEmpty(map)) {
			return;
		}

		final Set<Entry<Field, Object>> es = map.entrySet();
		for (final Entry<Field, Object> entry : es) {
			final Field field = entry.getKey();
			final Object object = entry.getValue();

			Object oldValue = null;
			try {
				field.setAccessible(true);
				oldValue = field.get(object);
				LOG.info("开始更新配置项key={},原value={},新value={}", name, oldValue, value);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}

			final Class<?> type = field.getType();

			// 1 先赋值为新值
			setValue(value, field, object, type);

			try {
				// 2 校验新值
				ZValidator.validatedAll(object, field);
			} catch (final Exception e) {

				final String message = Task.gExceptionMessage(e);
				LOG.error("更新新值{}异常,开始重置为旧值{},field={}.{},message={}", value, oldValue,
						object.getClass().getSimpleName(), field.getName(), message);

				// 3 如果新值校验不通过，则重新赋值为旧值
				setValue(oldValue, field, object, type);
			}


			LOG.info("更新配置项完成key={},新value={}", name, value);

		}
	}

	private static void setValue(final Object value, final Field field, final Object object, final Class<?> type) {
		if (type.getCanonicalName().equals(String.class.getCanonicalName())) {
			setValue(field, object, String.valueOf(value));
		} else if (type.getCanonicalName().equals(Byte.class.getCanonicalName())) {
			setValue(field, object, Byte.valueOf(String.valueOf(value)));
		} else if (type.getCanonicalName().equals(Short.class.getCanonicalName())) {
			setValue(field, object, Short.valueOf(String.valueOf(value)));
		} else if (type.getCanonicalName().equals(Integer.class.getCanonicalName())) {
			setValue(field, object, Integer.valueOf(String.valueOf(value)));
		} else if (type.getCanonicalName().equals(Long.class.getCanonicalName())) {
			setValue(field, object, Long.valueOf(String.valueOf(value)));
		} else if (type.getCanonicalName().equals(BigInteger.class.getCanonicalName())) {
			setValue(field, object, new BigInteger(String.valueOf(value)));
		} else if (type.getCanonicalName().equals(BigDecimal.class.getCanonicalName())) {
			setValue(field, object, new BigDecimal(String.valueOf(value)));
		} else if (type.getCanonicalName().equals(Boolean.class.getCanonicalName())) {
			setValue(field, object, Boolean.valueOf(String.valueOf(value)));
		} else if (type.getCanonicalName().equals(Double.class.getCanonicalName())) {
			setValue(field, object, Double.valueOf(String.valueOf(value)));
		} else if (type.getCanonicalName().equals(Float.class.getCanonicalName())) {
			setValue(field, object, Float.valueOf(String.valueOf(value)));
		} else if (type.getCanonicalName().equals(Character.class.getCanonicalName())) {
			setValue(field, object, Character.valueOf(String.valueOf(value).charAt(0)));
		} else {
			throw new IllegalArgumentException("@" + ZValue.class.getSimpleName() + " 字段 " + field.getName() + " 的类型 "
					+ field.getType().getSimpleName() + " 暂不支持");
		}
	}


	private static String getStringValue(final PropertiesConfiguration p, final String key) {
		final StringJoiner joiner = new StringJoiner(",");
		try {
			final String[] stringArray = p.getStringArray(key);
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

	private static void setValue(final Field field, final String fieldName, final Object object) {
		final PropertiesConfiguration p = ZProperties.getInstance();
		if (!p.containsKey(fieldName)) {
			return;
		}

		final Class<?> type = field.getType();
		if (type.getCanonicalName().equals(String.class.getCanonicalName())) {
			final String v1 = getStringValue(p, fieldName);
			setValue(field, object, v1);
		} else if (type.getCanonicalName().equals(Byte.class.getCanonicalName())) {
			setValue(field, object, p.getByte(fieldName));
		} else if (type.getCanonicalName().equals(Short.class.getCanonicalName())) {
			setValue(field, object, p.getShort(fieldName));
		} else if (type.getCanonicalName().equals(Integer.class.getCanonicalName())) {
			setValue(field, object, p.getInt(fieldName));
		} else if (type.getCanonicalName().equals(Long.class.getCanonicalName())) {
			setValue(field, object, p.getLong(fieldName));
		} else if (type.getCanonicalName().equals(BigInteger.class.getCanonicalName())) {
			setValue(field, object, p.getBigInteger(fieldName));
		} else if (type.getCanonicalName().equals(BigDecimal.class.getCanonicalName())) {
			setValue(field, object, p.getBigDecimal(fieldName));
		} else if (type.getCanonicalName().equals(Boolean.class.getCanonicalName())) {
			setValue(field, object, p.getBoolean(fieldName));
		} else if (type.getCanonicalName().equals(Double.class.getCanonicalName())) {
			setValue(field, object, p.getDouble(fieldName));
		} else if (type.getCanonicalName().equals(Float.class.getCanonicalName())) {
			setValue(field, object, p.getFloat(fieldName));
		} else if (type.getCanonicalName().equals(Character.class.getCanonicalName())) {
			setValue(field, object, p.getString(fieldName).charAt(0));
		} else {
			throw new IllegalArgumentException("@" + ZValue.class.getSimpleName() + " 字段 " + field.getName() + " 的类型 "
					+ field.getType().getSimpleName() + " 暂不支持");
		}
	}

	private static void setValue(final Field field, final ZValue zValue, final Object object) {
		setValue(field, zValue.name(), object);
	}

	private static void setValue(final Field field, final Object object, final Object value) {
		try {
			field.setAccessible(true);
			field.set(object, value);
			LOG.info("field赋值成功,field={},value={},object={}", field.getName(), value, object);
		} catch (IllegalArgumentException | IllegalAccessException  e) {
			e.printStackTrace();
		}
	}


}
