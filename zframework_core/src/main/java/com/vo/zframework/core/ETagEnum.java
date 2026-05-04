package com.vo.zframework.core;

/**
 * ETag 分类
 *
 * @author zhangzhen
 * @date 2025年12月22日 10:11:35
 */
public enum ETagEnum {
	
	STRONG {
		@Override
		public String handle(final String eTag) {
			return '\"' + eTag + '\"';
		}
	},

	WEAK {
		@Override
		public String handle(final String eTag) {
			return "W/" + ETagEnum.STRONG.handle(eTag);
		}
	};

	/**
	 * 从计算出的值处理为标准形式
	 * 
	 * @param eTag
	 * @return
	 */
	public abstract String handle(String eTag);
}
