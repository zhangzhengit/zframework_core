//package vo.vortex.scanner;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.nio.charset.Charset;
//import java.nio.file.FileSystems;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardWatchEventKinds;
//import java.nio.file.WatchEvent;
//import java.nio.file.WatchKey;
//import java.nio.file.WatchService;
//import java.util.Enumeration;
//import java.util.Properties;
//
//import vo.log.core.ZLog2;
//import vo.vortex.ZProperties;
//
///**
// * 配置文件监听器，监听配置变动，及时更新 @ZConfigurationProperties、 @ZValue 等
// *
// * @author zhangzhen
// * @date 2023年7月5日
// *
// */
//public class ZPropertiesListener {
//
//	private static final ZLog2 LOG = ZLog2.getInstance();
//
//	public static void listen(final String filePath) {
//
////		LOG.debug("配置热更新监听器启动,filePath={}", filePath);
//
//		final Thread thread = new Thread(task(filePath));
//		thread.setName(ZProperties.DEFALUT_PROPERTIES_NAME + "-Thread");
//		thread.start();
//
//	}
//
//	private static Runnable task(final String filePath) {
//		return () -> {
//			// 创建一个WatchService对象
//			WatchService watchService = null;
//			try {
//				watchService = FileSystems.getDefault().newWatchService();
//			} catch (final IOException e1) {
//				e1.printStackTrace();
//			}
//
//			// 获取文件所在的目录路径
//			final Path directory = Paths.get(filePath).getParent();
//
//			// 注册监听器，监听文件的修改事件
//			try {
//				directory.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
//			} catch (final IOException e1) {
//				e1.printStackTrace();
//			}
//
//			if (watchService == null) {
//				return;
//			}
//
//			// 启动一个无限循环，等待文件变化事件
//			while (true) {
//				WatchKey key;
//				try {
//					// 获取下一个文件变化事件
//					key = watchService.take();
//				} catch (final InterruptedException e) {
//					return;
//				}
//
//				// 遍历所有的文件变化事件
//				for (final WatchEvent<?> event : key.pollEvents()) {
//					final WatchEvent.Kind<?> kind = event.kind();
//
//					// 如果是文件修改事件
//					if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
//						// 获取文件名
//						final String fileName = event.context().toString();
//
//						if (fileName.equals(new File(filePath).getName())) {
//							readPAndUpdateNewValue(filePath);
//						}
//					}
//				}
//
//				// 重置WatchKey对象，以便继续接收文件变化事件
//				final boolean valid = key.reset();
//				if (!valid) {
//					break;
//				}
//			}
//		};
//
//	}
//
//	private static void readPAndUpdateNewValue(final String filePath) {
//
//		LOG.debug("配置文件修改了,开始更新");
//
//		final Properties properties = loadProperties(filePath);
//
//		final Enumeration<Object> keys = properties.keys();
//		while (keys.hasMoreElements()) {
//			final Object k = keys.nextElement();
//
//			final Object v = properties.get(k);
//
////			final String vS = String.valueOf(v);
////			final Object newValue = AppH.isExpression(vS) ? EE.execute(AppH.gExpression(vS)) : v;
//			final Object newValue = v;
//
//			try {
//				ZValueScanner.updateValueAndValidate(String.valueOf(k), newValue);
//			} catch (final Exception e) {
//				e.printStackTrace();
//				continue;
//			}
//		}
//	}
//
//	private static Properties loadProperties(final String filePath) {
//		// XXX 2026年5月22日 13:06:14 zhangzhen : 下面这段read的逻辑，想复用ZProperties类
//		// 但是怕又改出bug，先就这样吧
//
//		final Properties properties = new Properties();
//
//		try (final FileInputStream fileInputStream = new FileInputStream(new File(filePath));
//				InputStreamReader isr = new InputStreamReader(fileInputStream,
//						Charset.defaultCharset().displayName())) {
//			properties.load(isr);
//		} catch (final IOException e1) {
//			e1.printStackTrace();
//		}
//		return properties;
//	}
//
//}
