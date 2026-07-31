package vo.zframework.api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import vo.log.core.ZLog2;
import vo.zframework.common.STU;
import vo.zframework.compression.Brotli;
import vo.zframework.compression.CF;
import vo.zframework.compression.IZSTD;
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

	private final static ZLog2 LOG = ZLog2.getInstance();

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
		ex.shutdown();
	}

	private static void c() {

		final ServerConfigurationProperties scp = ZContext.getBean(ServerConfigurationProperties.class);

		if (!scp.isStaticResourcePreCompressionEnable()) {
			return;
		}

		final String staticResourcesPath = System.getProperty(ResourcesLoader.STATIC_RESOURCES_PROPERTY_NAME);

		if (!STU.hasContent(staticResourcesPath)) {
			return;
		}

		try (final Stream<Path> stream = Files.walk(Paths.get(staticResourcesPath))) {

			final Set<String> suffixSet = scp.getStaticResourcePreCompressionSuffix();

			final List<Path> ctp = stream
				.filter(Files::isRegularFile)
				.filter(path -> !isCompressedFile(path.getFileName().toString()))
				.filter(path -> exceedsCompressionMinLength(scp, path))
				.filter(path -> suffixSet.contains(getExtension(path)))
			    .collect(Collectors.toList());

			ctp.stream()
				.filter(path -> isTempFile(path.getFileName().toString()))
				.forEach(path -> {
					try {
						Files.delete(path);
					} catch (final IOException e) {
						LOG.warn("删除文件异常,path={}", path, e);
					}
				});

			final List<Path> cp = ctp.stream()
					.filter(path -> !isTempFile(path.getFileName().toString()))
					.collect(Collectors.toList());

			final Set<String> pcas = scp.getStaticResourcePreCompressionAlgorithm();

			// 压缩最快体积最大的gzip，用[核心-1]个线程把全部文件压缩完，让所有文件都尽快有压缩文件可用
			if (pcas.contains(AcceptEncodingEnum.GZIP.getValue())) {

				final List<Callable<String>> gzipcl = cp.stream().map(path -> (Callable<String>) () -> {
					compressGZIP(path);
					return "OK";
				}).collect(Collectors.toList());

				try (ExecutorService ex = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
					final List<Future<String>> gfl = ex.invokeAll(gzipcl);
					for (final Future<String> future : gfl) {
						try {
							future.get();
						} catch (final ExecutionException e) {
							LOG.warn("GZIP压缩失败,message={}", e.getCause().getMessage());
						}
					}
				}
			}

			// 压缩稍慢体积很小的zstd，用[核心-1]个线程把全部文件压缩完，让尽快有更小的压缩文件可用
			if (pcas.contains(AcceptEncodingEnum.ZSTD.getValue())) {

				final List<Callable<String>> zstdcl = cp.stream().map(path -> (Callable<String>) () -> {
					compressZSTD(path);
					return "OK";
				}).collect(Collectors.toList());

				try (ExecutorService ex = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
					final List<Future<String>> zstdl = ex.invokeAll(zstdcl);
					for (final Future<String> future : zstdl) {
						try {
							future.get();
						} catch (final ExecutionException e) {
							LOG.warn("ZSTD压缩失败,message={}", e.getCause().getMessage());
						}
					}
				}
			}

			// 压缩慢得受不了体积最小的br，用当前[1]个线程(非main线程)把全部文件慢慢去压缩，
			// 体积比zstd小不了多少，但是耗时可能是zstd的几十上百倍，就赶快释放cpu慢慢压缩去吧
			if (pcas.contains(AcceptEncodingEnum.BR.getValue())) {
				for (final Path path : cp) {
					compressBR(path);
				}
			}

		} catch (IOException | InterruptedException e1) {
			e1.printStackTrace();
		}

	}

	private static boolean exceedsCompressionMinLength(final ServerConfigurationProperties scp, final Path path) {
		try {
			return Files.size(path) >= (scp.getCompressionMinLength() * 1024);
		} catch (final IOException e) {
			LOG.warn("获取文件大小异常,跳过压缩,path={}", path, e);
		}

		return false;
	}

	private static boolean isTempFile(final String fileName) {
		return fileName.endsWith(BR_TEMP_SUFFIX)
		    || fileName.endsWith(ZSTD_TEMP_SUFFIX)
		    || fileName.endsWith(GZIP_TEMP_SUFFIX);
	}

	private static boolean isCompressedFile(final String fileName) {
		return fileName.endsWith(BR_SUFFIX)
			|| fileName.endsWith(ZSTD_SUFFIX)
			|| fileName.endsWith(GZIP_SUFFIX);
	}

	private static void compressGZIP(final Path source) throws IOException {

		final long sourceLastModified = Files.getLastModifiedTime(source).toMillis();

		final Path gzip = source.resolveSibling(source.getFileName() + GZIP_SUFFIX);
		if (!Files.exists(gzip)) {
			compressZGzip0(source);
		} else if ((sourceLastModified >= Files.getLastModifiedTime(gzip).toMillis())) {
			Files.delete(gzip);
			compressZGzip0(source);
		}

	}

	private static void compressZSTD(final Path source) throws IOException {

		final long sourceLastModified = Files.getLastModifiedTime(source).toMillis();

		final Path zstd = source.resolveSibling(source.getFileName() + ZSTD_SUFFIX);
		if (!Files.exists(zstd)) {
			compressZSTD0(source);
		} else if ((sourceLastModified >= Files.getLastModifiedTime(zstd).toMillis())) {
			Files.delete(zstd);
			compressZSTD0(source);
		}

	}

	private static void compressBR(final Path source) throws IOException {

		final long sourceLastModified = Files.getLastModifiedTime(source).toMillis();

		final Path br = source.resolveSibling(source.getFileName() + BR_SUFFIX);
		if (!Files.exists(br)) {
			compressBR0(source);
		} else if ((sourceLastModified >= Files.getLastModifiedTime(br).toMillis())) {
			Files.delete(br);
			compressBR0(source);
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

	private static void compressBR0(final Path source) throws IOException {
		if (Brotli.isAvailable()) {
			final Path targetTEMPBR = source.resolveSibling(source.getFileName() + BR_TEMP_SUFFIX);
			Brotli.compressFile(source, targetTEMPBR);

			final Path brp = source.resolveSibling(source.getFileName() + BR_SUFFIX);
			Files.move(targetTEMPBR, brp, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void compressZSTD0(final Path source) throws IOException {
		final Path targetTEMPZSTD = source.resolveSibling(source.getFileName() + ZSTD_TEMP_SUFFIX);

		final IZSTD zstd = ZContext.getBean(IZSTD.class);
		zstd.compressFile(source, targetTEMPZSTD);

		final Path brp = source.resolveSibling(source.getFileName() + ZSTD_SUFFIX);
		Files.move(targetTEMPZSTD, brp, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
	}

	private static void compressZGzip0(final Path source) throws IOException {
		final Path targetTEMPGZIP = source.resolveSibling(source.getFileName() + GZIP_TEMP_SUFFIX);
		ZGzip.compressFile(source, targetTEMPGZIP);

		final Path brp = source.resolveSibling(source.getFileName() + GZIP_SUFFIX);
		Files.move(targetTEMPGZIP, brp, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
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
