package com.vo.core;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * 检查端口是否被占用
 *
 * @author zhangzhen
 * @date 2024年12月31日 下午6:17:22
 *
 */
public class PortChecker {

	public static boolean isPortInUse(final int port) {
		try (ServerSocket socket = new ServerSocket(port)) {
			return false;
		} catch (final IOException e) {
			return true;
		}
	}
}
