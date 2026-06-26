package vo.zframework.api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import vo.zframework.common.STU;
import vo.zframework.compression.Brotli;
import vo.zframework.compression.CF;
import vo.zframework.compression.ZGzip;
import vo.zframework.configuration.properties.ServerConfigurationProperties;
import vo.zframework.core.ZContext;
import vo.zframework.enums.AcceptEncodingEnum;
import vo.zframework.html.ResourcesLoader;

/**
 *
 * 静态资源预压缩
 *
 * @author zhangzhen
 * @date 2026年6月25日 11:29:09
 */
public class StaticResourcesPreCompressionService {

	private static final String _TEMP = "_TEMP";
	public static final String BR_SUFFIX = "._vo_br";
	public static final String BR_TEMP_SUFFIX = BR_SUFFIX + _TEMP;
	public static final String ZSTD_SUFFIX = "._vo_zstd";
	public static final String ZSTD_TEMP_SUFFIX = ZSTD_SUFFIX + _TEMP;
	public static final String GZIP_SUFFIX = "._vo_gzip";
	public static final String GZIP_TEMP_SUFFIX = GZIP_SUFFIX + _TEMP;

	public static void preCompression() {
		final ExecutorService ex = Executors.newSingleThreadExecutor();
		ex.execute(StaticResourcesPreCompressionService::c);
	}

	private static void c() {

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
				if (fileName.endsWith(BR_SUFFIX)
				|| fileName.endsWith(ZSTD_SUFFIX)
				|| fileName.endsWith(GZIP_SUFFIX)
						) {
					continue;
				}

				// 删除临时文件
				if (fileName.endsWith(BR_TEMP_SUFFIX)
				|| fileName.endsWith(ZSTD_TEMP_SUFFIX)
				|| fileName.endsWith(GZIP_TEMP_SUFFIX)
						) {
					Files.delete(path);
					continue;
				}

				// 低于压缩阈值
				if (Files.size(path) < (cp.getCompressionMinLength() * 1024)) {
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
		final Path gzip = source.resolveSibling(source.getFileName() + GZIP_SUFFIX);
		if (!Files.exists(gzip)) {
			compressZGzip(source);
		} else if ((sourceLastModified >= Files.getLastModifiedTime(gzip).toMillis())) {
			Files.delete(gzip);
			compressZGzip(source);
		}

		// zstd
		final Path zstd = source.resolveSibling(source.getFileName() + ZSTD_SUFFIX);
		if (!Files.exists(zstd)) {
			compressZSTD(source);
		} else if ((sourceLastModified >= Files.getLastModifiedTime(zstd).toMillis())) {
			Files.delete(zstd);
			compressZSTD(source);
		}

		// br 最耗时，放最后
		final Path br = source.resolveSibling(source.getFileName() + BR_SUFFIX);
		if (!Files.exists(br)) {
			compressBR(source);
		} else if ((sourceLastModified >= Files.getLastModifiedTime(br).toMillis())) {
			Files.delete(br);
			compressBR(source);
		}

	}

	public static List<CF> gCFOrderByFileLength(final File sourceFile) {
		final File br = new File(sourceFile + StaticResourcesPreCompressionService.BR_SUFFIX);
		final File zstd = new File(sourceFile + StaticResourcesPreCompressionService.ZSTD_SUFFIX);
		final File gzip = new File(sourceFile + StaticResourcesPreCompressionService.GZIP_SUFFIX);

		final List<CF> list = new ArrayList<>(4);

		// 为了配合response.body，用null，不用IDENTITY
		list.add(new CF(sourceFile, sourceFile.length(), null));

		if (br.exists()) {
			list.add(new CF(br, br.length(), AcceptEncodingEnum.BR));
		}
		if (zstd.exists()) {
			list.add(new CF(zstd, zstd.length(), AcceptEncodingEnum.ZSTD));
		}
		if (gzip.exists()) {
			list.add(new CF(gzip, gzip.length(), AcceptEncodingEnum.GZIP));
		}

		if (list.size() <= 1) {
			return list;
		}

		list.sort(Comparator.comparing(CF::getFileSize));

		return list;
	}


	private static void compressBR(final Path source) throws IOException {
		if (Brotli.isAvailable()) {
			final Path targetTEMPZSTD = source.resolveSibling(source.getFileName() + BR_TEMP_SUFFIX);
			Brotli.compressFile(source, targetTEMPZSTD);

			final Path brp = targetTEMPZSTD.resolveSibling(String.valueOf(targetTEMPZSTD.getFileName())
					.replace(StaticResourcesPreCompressionService.BR_TEMP_SUFFIX,
							StaticResourcesPreCompressionService.BR_SUFFIX));
			Files.move(targetTEMPZSTD, brp);
		}
	}

	private static void compressZSTD(final Path source) throws IOException {
		final Path targetTEMPZSTD = source.resolveSibling(source.getFileName() + ZSTD_TEMP_SUFFIX);
		vo.zframework.compression
		.ZSTD.compressFile(source, targetTEMPZSTD);

		final Path brp = targetTEMPZSTD.resolveSibling(String.valueOf(targetTEMPZSTD.getFileName())
				.replace(StaticResourcesPreCompressionService.ZSTD_TEMP_SUFFIX, StaticResourcesPreCompressionService.ZSTD_SUFFIX));
		Files.move(targetTEMPZSTD, brp);
	}

	private static void compressZGzip(final Path source) throws IOException {
		final Path targetTEMPGZIP = source.resolveSibling(source.getFileName() + GZIP_TEMP_SUFFIX);
		ZGzip.compressFile(source, targetTEMPGZIP);

		final Path brp = targetTEMPGZIP.resolveSibling(String.valueOf(targetTEMPGZIP.getFileName())
				.replace(StaticResourcesPreCompressionService.GZIP_TEMP_SUFFIX, StaticResourcesPreCompressionService.GZIP_SUFFIX));
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
