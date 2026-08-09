package vo.vortex.bean;

/**
 *  
 *
 * @author zhangzhen
 * @date 2023年11月4日
 * 
 */
public interface ZBeanPostProcessor {
//	@Nullable
	default Object postProcessBeforeInitialization(final Object bean, final String beanName)  {
		return bean;
	}
//	@Nullable
	default Object postProcessAfterInitialization(final Object bean, final String beanName)  {
		return bean;
	}
}
