package com.vo.scanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Enumeration;
import java.util.Properties;

import com.vo.configuration.ZProperties;
import com.vo.core.ZLog2;
import com.votool.ze.ZE;
import com.votool.ze.ZES;

/**
 * 配置文件监听器，监听配置变动，及时更新 @ZConfigurationProperties、 @ZValue 等
 *
 * @author zhangzhen
 * @date 2023年7月5日
 *
 */
public class ZPropertiesListener {

	private static final ZLog2 LOG = ZLog2.getInstance();
	private static final ZE ZE = ZES.newZE(1, ZProperties.PROPERTIES_NAME + "-Thread-");

	public static void listen(final String filePath) {

		LOG.info("配置热更新监听器启动,filePath={}", filePath);

		ZE.executeInQueue(() -> {

			// 创建一个WatchService对象
			WatchService watchService = null;
			try {
				watchService = FileSystems.getDefault().newWatchService();
			} catch (final IOException e1) {
				e1.printStackTrace();
			}

			// 获取文件所在的目录路径
			final Path directory = Paths.get(filePath).getParent();

			// 注册监听器，监听文件的修改事件
			try {
				directory.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
			} catch (final IOException e1) {
				e1.printStackTrace();
			}

			if (watchService == null) {
				return;
			}

			// 启动一个无限循环，等待文件变化事件
			while (true) {
				WatchKey key;
				try {
					// 获取下一个文件变化事件
					key = watchService.take();
				} catch (final InterruptedException e) {
					return;
				}

				// 遍历所有的文件变化事件
				for (final WatchEvent<?> event : key.pollEvents()) {
					final WatchEvent.Kind<?> kind = event.kind();

					// 如果是文件修改事件
					if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
						// 获取文件名
						final String fileName = event.context().toString();

						if (fileName.equals(new File(filePath).getName())) {
							try {
								final Properties properties = new Properties();

								final FileInputStream fileInputStream = new FileInputStream(new File(filePath));
								final InputStreamReader isr = new InputStreamReader(fileInputStream,
										Charset.defaultCharset().displayName());
								properties.load(isr);
								final Enumeration<Object> keys = properties.keys();
								while(keys.hasMoreElements()) {
									final Object k = keys.nextElement();

									final Object v = properties.get(k);

									try {
										ZValueScanner.updateValue(String.valueOf(k), v);
									} catch (final Exception e) {
										e.printStackTrace();
										continue;
									}
								}

							} catch (final IOException e) {
								e.printStackTrace();
							}
						}
					}
				}

				// 重置WatchKey对象，以便继续接收文件变化事件
				final boolean valid = key.reset();
				if (!valid) {
					break;
				}
			}

		});

	}

}
