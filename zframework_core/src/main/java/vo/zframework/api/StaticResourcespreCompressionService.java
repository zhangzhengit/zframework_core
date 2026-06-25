package vo.zframework.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import vo.zframework.common.STU;
import vo.zframework.compression.Brotli;
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

	public static void preCompression() {
		final String staticResourcesPath = System.getProperty(ResourcesLoader.STATIC_RESOURCES_PROPERTY_NAME);
		if (!STU.hasContent(staticResourcesPath)) {
			return;
		}

		final ExecutorService ex = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		try {
			final Stream<Path> stream = Files.walk(Paths.get(staticResourcesPath));
			stream
			.filter(Files::isRegularFile)
			// 跳过.br文件(已经压缩好的)
			.filter(p -> !String.valueOf(p.getFileName()).endsWith(BR))
			.filter(p -> !Files.exists(p.resolveSibling(p.getFileName() + BR)))
			.forEach(p -> {
				final Path source = p.toAbsolutePath();
				if (String.valueOf(source.getFileName()).endsWith(TEMP_BR)) {
					try {
						Files.deleteIfExists(source);
					} catch (final IOException e) {
						e.printStackTrace();
					}
				} else {
					final Path targetTEMPBR = source.resolveSibling(source.getFileName() + TEMP_BR);
					ex.execute(() -> Brotli.compressFile(source, targetTEMPBR));
				}
			});

		} catch (final IOException e) {
			e.printStackTrace();
		}

	}

}
