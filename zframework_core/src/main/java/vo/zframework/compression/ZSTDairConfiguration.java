package vo.zframework.compression;

import vo.zframework.anno.ZBean;
import vo.zframework.anno.ZConfiguration;
import vo.zframework.anno.ZOrder;
import vo.zframework.core.ZContext;

/**
 *
 *
 * @author zhangzhen
 * @date 2026年7月4日 20:02:26
 */
@ZConfiguration
@ZOrder(value = Integer.MIN_VALUE)
public class ZSTDairConfiguration {

	@ZBean
	public IZSTD zstdair() {
		final IZSTD izstd = new ZSTDair();
		ZContext.addBean(IZSTD.class, izstd);
		return izstd;
	}
}
