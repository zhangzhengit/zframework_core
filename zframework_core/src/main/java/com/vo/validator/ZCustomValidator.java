package com.vo.validator;

import java.lang.reflect.Field;

/**
 * 自定义校验注解的抽象类，所有自定义注解要继承此类
 *
 * 注意：自定义注解的所有校验逻辑都要自己实现，不像内置的校验注解
 * 如： @ZNotEmtpy 自动引入了 @ZNotNull 的功能。
 *
 * @author zhangzhen
 * @date 2023年11月14日
 *
 */
public interface ZCustomValidator {

	public void validated(final Object object, final Field field) throws Exception;

}
