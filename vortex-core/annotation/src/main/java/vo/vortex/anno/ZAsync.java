package vo.vortex.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在方法上，表示此方法异步执行
 * 带有本注解的方法必须声明为public修饰
 *
 * 对于无返回值的方法，正常写即可；
 *
 * 对于有返回值的方法，必须使用 ZAsyncRV<T> 作为返回类型，
 * 		并且使用 ZAsyncRV.ok(T v) 来构造返回值
 *
 * @author zhangzhen
 * @date 2023年7月8日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ZAsync {

}
