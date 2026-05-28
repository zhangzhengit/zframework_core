package com.vo.zframework.core;

/**
 *	PD
 *
 * @author zhangzhen
 * @date 2026年5月28日 17:27:24
 */
public class PDTL {

	private final static ThreadLocal<PD> TL = new ThreadLocal<>();

	public static void set(final PD pd) {
		TL.set(pd);
	}

	public static PD get() {
		return TL.get();
	}

}
