package vo.zframework.aop;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import vo.zframework.anno.ZAOP;
import vo.zframework.anno.ZSynchronously;
import vo.zframework.common.RU;
import vo.zframework.exception.ZSynchronouslyAOPException;

/**
 * @ZSynchronously 的AOP类
 *
 * @author zhangzhen
 * @date 2023年10月28日
 *
 */
@ZAOP(interceptType = ZSynchronously.class)
public class ZSynchronouslyAOP implements ZIAOP {

	@Override
	public Object before(final AOPParameter AOPParameter) {
		return null;
	}

	@Override
	public Object around(final AOPParameter AOPParameter) {

		final String value = ZSynchronouslyAOP.gValue(AOPParameter);

		synchronized (("AOPLock" + value).intern()) {
			final Object v = AOPParameter.invoke();
			return v;
		}

	}

	private static String gValue(final AOPParameter AOPParameter) {
		final ZSynchronously synchronously = AOPParameter.getMethod().getDeclaredAnnotation(ZSynchronously.class);

		final String key = synchronously.key();

		final Method method = AOPParameter.getMethod();
		final Parameter[] parameters = method.getParameters();

		return ZSynchronouslyAOP.getKeyValue(AOPParameter, key, parameters);
	}

	private static String getKeyValue(final AOPParameter AOPParameter, final String key, final Parameter[] parameters) {

		final String p = AOPParameter.getTarget().getClass().getCanonicalName()
				+ "@" + AOPParameter.getMethodName()
				+ "@" + key
				;

		for (int i = 0; i < parameters.length; i++) {
			final Parameter parameter = parameters[i];
			final String name = parameter.getName();
			if (name.equals(key)) {
				final List<Object> pl = AOPParameter.getParameterList();
				final Object a = pl.get(i);
				return p +'='+ a;
			}

			if (key.startsWith(name)) {
				final int x = key.indexOf(".");
				if (x > -1) {
					final List<Object> pl = AOPParameter.getParameterList();
					final Object a = pl.get(i);
					final String fieldName = key.substring(x + 1);

					final Field field = RU.getDeclaredField(a, fieldName);
					final Object v = RU.getFiledValue(a, field);

					return p + '=' + v;
				}
			}
		}

		// FIXME 2023年10月28日 上午1:15:39 zhanghen: TODO 启动时先校验 key是否存在（是否匹配参数名或参数名.字段名）
		// 不要等执行时在此抛异常
		throw new ZSynchronouslyAOPException("key 指定参数名不存在");
	}

	@Override
	public Object after(final AOPParameter AOPParameter) {
		return null;
	}

	public static boolean isJavaLangClass(final Object object) {
		return object.getClass().getCanonicalName().startsWith("java.lang");
	}
}
