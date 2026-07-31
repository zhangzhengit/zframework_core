package vo.vortex.compression;

import vo.vortex.anno.ZBean;
import vo.vortex.anno.ZConfiguration;
import vo.vortex.anno.ZOrder;
import vo.vortex.core.ZContext;

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
