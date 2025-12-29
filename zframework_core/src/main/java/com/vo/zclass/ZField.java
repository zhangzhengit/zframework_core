package com.vo.zclass;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 表示java类的一个字段，调用 toString 获取String形式的结果
 *
 * @author zhangzhen
 * @date 2023年6月16日
 *
 */
public class ZField {

	// FIXME 2025年9月23日 下午8:45:43 zhangzhen: 这个类考虑好，哪些修饰符是不允许同时出现的，加以限制
	
	/**
	 * 	权限修饰符
	 */
	private PermissionEnum permissionEnum = PermissionEnum.DEFAULT;

	/**
	 * 	String 形式表达的类型，如：字段类型为String，则可以构造传值 String.class.getName() 
	 */
	private final String type;

	/**
	 * 	字段名称
	 */
	private final String name;

	/**
	 * 	字段值
	 */
	private final Object value;
	
	
	private final boolean isStatic;
	
	private final boolean isFinal;
	
	private final boolean isVolatile;
	
	private final boolean isTransient ;
	
	/**
	 * 	字段所带的注解
	 */
	private List<String> annoList;

	/**
	 * 使用字符串形式添加一个注解
	 * 
	 * @param annotationString
	 */
	public void addAnnotation(final String annotationString) {
		this.initAnnoList();
		this.annoList.add(annotationString);
	}

	/**
	 * 使用Class形式添加一个注解
	 * 
	 * @param annotationClass
	 */
	public void addAnnotation(final Class<? extends Annotation> annotationClass) {
		this.initAnnoList();
		this.annoList.add("@" + annotationClass.getCanonicalName());
	}

	private void initAnnoList() {
		if(this.annoList == null) {
			this.annoList = new ArrayList<>();
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		
		// 先 生成注解
		if (this.annoList != null) {
			for (final String as : this.annoList) {
				builder.append(as).append(ZClass.NEW_LINE);
			}
		}
		
		
		final PermissionEnum pe = this.getPermissionEnum();
		if (pe != null) {
			builder.append(pe.getV()).append(" ");
		}

		if (this.isStatic) {
			builder.append("static ");
		}
		if (this.isFinal) {
			builder.append("final ");
		}
		if (this.isVolatile) {
			builder.append("volatile ");
		}
		if (this.isTransient) {
			builder.append("tracisent ");
		}
		
		
		// type value;
		builder.append(this.getType()).append(" ").append(this.getName());
		
		if (this.getValue() != null) {
			builder.append(" = ").append("(").append(this.getType()).append(")").append(this.getValue()).append(";");
		} else {
			builder.append(";");
		}

		return builder.toString();
	}

	
	/**
	 * 构造一个最简单的java 字段表示，如：String name;
	 * 则可以构造为 ZField(String.class.getName(), "name");
	 * 		  或者 ZField("String", "name");
	 * 
	 * @param type
	 * @param name
	 */
	public ZField(final String type, final String name) {
		super();
		this.type = type;
		this.name = name;
		this.value = null;
		this.isFinal = false;
		this.isStatic = false;
		this.isVolatile = false;
		this.isTransient = false;
	}

	
	public ZField(final PermissionEnum permissionEnum, final String type, final String name) {
		super();
		if (permissionEnum != null) {
			this.permissionEnum = permissionEnum;
		}
		this.type = type;
		this.name = name;
		this.value = null;
		this.isFinal = false;
		this.isStatic = false;
		this.isVolatile = false;
		this.isTransient = false;
	}

	
	public ZField(final PermissionEnum permissionEnum, final String type, final String name, final Object value, final boolean isStatic,
			final boolean isFinal, final boolean isVolatile) {
		super();
		this.permissionEnum = permissionEnum;
		this.type = type;
		this.name = name;
		this.value = value;
		this.isStatic = isStatic;
		this.isFinal = isFinal;
		this.isVolatile = isVolatile;
		this.isTransient = false;
	}

	public ZField(final PermissionEnum permissionEnum, final String type, final String name, final Object value, final boolean isStatic,
			final boolean isFinal, final boolean isVolatile, final List<String> annoList) {
		super();
		this.permissionEnum = permissionEnum;
		this.type = type;
		this.name = name;
		this.value = value;
		this.isStatic = isStatic;
		this.isFinal = isFinal;
		this.isVolatile = isVolatile;
		this.isTransient = false;
		this.annoList = annoList;
	}

	public ZField(final String type, final String name, final Object value) {
		super();
		this.type = type;
		this.name = name;
		this.value = value;
		this.isFinal = false;
		this.isStatic = false;
		this.isVolatile = false;
		this.isTransient = false;
	}


	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public Object getValue() {
		return value;
	}

	public List<String> getAnnoList() {
		return annoList;
	}

	public void setAnnoList(final List<String> annoList) {
		this.annoList = annoList;
	}

	@Override
	public int hashCode() {
		return Objects.hash(annoList, name, type, value);
	}

	
	public PermissionEnum getPermissionEnum() {
		return permissionEnum;
	}

	public void setPermissionEnum(final PermissionEnum permissionEnum) {
		this.permissionEnum = permissionEnum;
	}


	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ZField other = (ZField) obj;
		return Objects.equals(annoList, other.annoList) && Objects.equals(name, other.name)
				&& Objects.equals(type, other.type) && Objects.equals(value, other.value);
	}

}
