package vo.zframework.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import vo.zframework.common.STU;
import vo.zframework.compression.Brotli;
import vo.zframework.compression.ZGzip;
import vo.zframework.configuration.properties.ServerConfigurationProperties;
import vo.zframework.core.ZContext;
import vo.zframework.html.ResourcesLoader;

/**
 *
 * 静态资源预压缩
 *
 * @author zhangzhen
 * @date 2026年6月25日 11:29:09
 */
public class StaticResourcespreCompressionService {

	public static final String BR = ".br";
	public static final String TEMP_BR = ".tempbr";
	public static final String GZIP = ".gzip";
	public static final String TEMP_GZIP = ".tempgzip";
	public static final String ZSTD = ".zstd";
	public static final String TEMP_ZSTD = ".tempzstd";

	public static void preCompression() {
		final ExecutorService ex = Executors.newSingleThreadExecutor();
		ex.execute(StaticResourcespreCompressionService::extracted);
	}

	private static void extracted() {

		final String staticResourcesPath = System.getProperty(ResourcesLoader.STATIC_RESOURCES_PROPERTY_NAME);

		if (!STU.hasContent(staticResourcesPath)) {
			return;
		}

		final int availableProcessors = Runtime.getRuntime().availableProcessors();
		final int threads = availableProcessors <= 2 ? 1 : availableProcessors / 2;
		final ExecutorService ex = Executors.newFixedThreadPool(threads);

		final ServerConfigurationProperties cp = ZContext.getBean(ServerConfigurationProperties.class);

		try (final Stream<Path> stream = Files.walk(Paths.get(staticResourcesPath))) {

			final List<Path> pl = stream.collect(Collectors.toList());
			for (final Path path : pl) {

				if (!Files.isRegularFile(path)) {
					continue;
				}

				final String fileName = path.getFileName().toString();
				// 跳过已压缩好的文件
				if (fileName.endsWith(BR)
				|| fileName.endsWith(ZSTD)
				|| fileName.endsWith(GZIP)
						) {
					continue;
				}

				// 删除临时文件
				if (fileName.endsWith(TEMP_BR)
				|| fileName.endsWith(TEMP_ZSTD)
				|| fileName.endsWith(TEMP_GZIP)
						) {
					Files.delete(path);
					continue;
				}

				// 跳过配置[不压缩]的文件
				final Set<String> suffixSet = cp.getStaticResourcePreCompressionSuffix();
				final String extension = getExtension(path);
				if (!suffixSet.contains(extension)) {
					continue;
				}

				ex.execute(() -> {
					try {
						compress(path);
					} catch (final IOException e) {
						e.printStackTrace();
					}
				});
			}

		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private static void compress(final Path source) throws IOException {

		// 看下加后缀的是否存在，不存在则压缩；或者原文件修改时间晚于压缩文件，也重新压缩

		final long sourceLastModified = Files.getLastModifiedTime(source).toMillis();

		// 最快的gzip先执行，让尽快有压缩文件可用
		final Path gzip = source.resolveSibling(source.getFileName() + GZIP);
		if (!Files.exists(gzip)) {
			compressZGzip(source);
		} else if ((sourceLastModified >= Files.getLastModifiedTime(gzip).toMillis())) {
			Files.delete(gzip);
			compressZGzip(source);
		}

		// zstd
		final Path zstd = source.resolveSibling(source.getFileName() + ZSTD);
		if (!Files.exists(zstd)) {
			compressZSTD(source);
		} else if ((sourceLastModified >= Files.getLastModifiedTime(zstd).toMillis())) {
			Files.delete(zstd);
			compressZSTD(source);
		}

		// br 最耗时，放最后
		final Path br = source.resolveSibling(source.getFileName() + BR);
		if (!Files.exists(br)) {
			compressBR(source);
		} else if ((sourceLastModified >= Files.getLastModifiedTime(br).toMillis())) {
			Files.delete(br);
			compressBR(source);
		}

	}

	private static void compressBR(final Path source) throws IOException {
		if (Brotli.isAvailable()) {
			final Path targetTEMPZSTD = source.resolveSibling(source.getFileName() + TEMP_BR);
			Brotli.compressFile(source, targetTEMPZSTD);

			final Path brp = targetTEMPZSTD.resolveSibling(String.valueOf(targetTEMPZSTD.getFileName())
					.replace(StaticResourcespreCompressionService.TEMP_BR,
							StaticResourcespreCompressionService.BR));
			Files.move(targetTEMPZSTD, brp);
		}
	}

	private static void compressZSTD(final Path source) throws IOException {
		final Path targetTEMPZSTD = source.resolveSibling(source.getFileName() + TEMP_ZSTD);
		vo.zframework.compression
		.ZSTD.compressFile(source, targetTEMPZSTD);

		final Path brp = targetTEMPZSTD.resolveSibling(String.valueOf(targetTEMPZSTD.getFileName())
				.replace(StaticResourcespreCompressionService.TEMP_ZSTD, StaticResourcespreCompressionService.ZSTD));
		Files.move(targetTEMPZSTD, brp);
	}

	private static void compressZGzip(final Path source) throws IOException {
		final Path targetTEMPGZIP = source.resolveSibling(source.getFileName() + TEMP_GZIP);
		ZGzip.compressFile(source, targetTEMPGZIP);

		final Path brp = targetTEMPGZIP.resolveSibling(String.valueOf(targetTEMPGZIP.getFileName())
				.replace(StaticResourcespreCompressionService.TEMP_GZIP, StaticResourcespreCompressionService.GZIP));
		Files.move(targetTEMPGZIP, brp);
	}

	private static String getExtension(final Path path) {
		final String fileName = path.getFileName().toString();
		final int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex <= 0) {
			return "";
		}

		return fileName.substring(dotIndex + 1).toLowerCase();
	}

}
