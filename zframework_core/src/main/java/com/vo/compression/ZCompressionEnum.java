package com.vo.compression;

/**
 *
 *
 * @author zhangzhen
 * @date 2025年1月2日 下午8:28:53
 *
 */
public enum ZCompressionEnum {

	/**
	 * 广泛支持，兼容性极好，压缩速度适中，压缩率比 Deflate 稍高
	 */
	GZIP,

	/**
	 * 与 Gzip 类似，Deflate 主要用于 HTTP 压缩，但通常压缩率较低，因此现在在 Web 上不如 Gzip 和 Brotli 常见，
	 * 解压速度快，压缩过程相对较轻便，适用于资源有限的环境
	 */
	DEFLATE,

	/**
	 * 主要用于 HTTPS 的响应压缩，尤其在传输文本内容（如 HTML、CSS、JavaScript）时能提供更高的压缩率，
	 * 压缩率高，尤其适合文本类数据；解压速度也比较快
	 */
	BR,

	/**
	 * 更多用于服务器端数据存储或其他非浏览器场景（如文件压缩），高压缩率，高速解压，
	 * 广泛用于现代压缩工具中，但浏览器支持度较差。
	 */
	ZSTD,

	;
}
