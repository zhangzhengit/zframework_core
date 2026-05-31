package vo.zframework.zclass;

import java.lang.reflect.InvocationTargetException;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.SimpleCompiler;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月11日
 *
 */
public class ZCU {

	public static final String ZCLASS = "ZClass";

	private static final String PACKAGE = "package ";

	/**
	 * 从字符串形式的java代码来构造一个对象
	 *
	 * @param source
	 * @param package1
	 * @param className
	 * @return
	 */
	public static Object newInstance(final String source, final String package1, final String className) {
		final SimpleCompiler compiler = new SimpleCompiler();
		try {

			compiler.cook(source);

			final String cN = package1.substring(package1.indexOf(PACKAGE) + PACKAGE.length()) + "." + className;
			final Class<?> cls = compiler.getClassLoader().loadClass(cN);
			final Object newInstance = cls.getDeclaredConstructor().newInstance();

			return newInstance;

		} catch (final CompileException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e1) {
			e1.printStackTrace();
		}

		return null;

	}

}
