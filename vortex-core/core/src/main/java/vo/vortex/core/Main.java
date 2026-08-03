package vo.vortex.core;

import java.time.LocalDateTime;

/**
 *
 *
 * @author zhangzhen
 * @date 2026年7月31日 17:49:08
 */
public class Main {

	public static void main(final String[] args) {
		System.out.println(LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "Main.main()");

		// 直接引用生成的类
//        final Class<?> cls = vo.test_native.generated.ARegistry.class;
//        System.out.println(cls.getName());
//			final Class<?> cls = vo.vortex.apt.generated.ZNotNullRegistry.class;
	}
}
