package vo.zframework.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

		final ServerConfigurationProperties cp = ZContext.getBean(ServerConfigurationProperties.class);

		try (final Stream<Path> stream = Files.walk(Paths.get(staticResourcesPath))) {
				stream
				.parallel()
				.filter(Files::isRegularFile)
				.filter(p -> {
					try {
						return Files.size(p) >= (cp.getCompressionMinLength() * 1024);
					} catch (final IOException e) {
						e.printStackTrace();
					}
					return false;
				})
				// 跳过.br/.gzip文件(已经压缩好的)
				.filter(p -> !String.valueOf(p.getFileName()).endsWith(BR))
				.filter(p -> !String.valueOf(p.getFileName()).endsWith(GZIP))
				.filter(p -> !String.valueOf(p.getFileName()).endsWith(ZSTD))
				.filter(p -> !Files.exists(p.resolveSibling(p.getFileName() + BR)))
				.filter(p -> !Files.exists(p.resolveSibling(p.getFileName() + GZIP)))
				.filter(p -> !Files.exists(p.resolveSibling(p.getFileName() + ZSTD)))
				.forEach(p -> {
				final Path source = p.toAbsolutePath();

				final Set<String> suffixset = cp.getStaticResourcePreCompressionSuffix();
				final String extension = getExtension(source);
				if (!suffixset.contains(extension)) {
					return;
				}

//				System.out.println("source = " + source);
				if (	String.valueOf(source.getFileName()).endsWith(TEMP_BR)
					||  String.valueOf(source.getFileName()).endsWith(TEMP_GZIP)
						) {
					try {
//						System.out.println("删除TEMp文件 = " + source);
						Files.deleteIfExists(source);
					} catch (final IOException e) {
						e.printStackTrace();
					}
				} else {

					final Path targetTEMPZSTD = source.resolveSibling(source.getFileName() + TEMP_ZSTD);
					vo.zframework.compression
					.ZSTD.compressFile(source,targetTEMPZSTD);

					final Path targetTEMPGZIP = source.resolveSibling(source.getFileName() + TEMP_GZIP);
//					System.out.println("targetTEMPGZIP = " + targetTEMPGZIP);
					ZGzip.compressFile(source, targetTEMPGZIP);


					// br 最耗时，放最后
					final Path targetTEMPBR = source.resolveSibling(source.getFileName() + TEMP_BR);
//					System.out.println("targetTEMPBR = " + targetTEMPBR);
					Brotli.compressFile(source, targetTEMPBR);
				}
			});

		} catch (final IOException e) {
			e.printStackTrace();
		}
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
