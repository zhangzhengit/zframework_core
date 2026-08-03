package vo.vortex.g;

import java.io.InputStream;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 读取vortex-maven-plugin插件读取到的Class信息
 *
 * @author zhangzhen
 * @date 2026年8月3日 03:19:24
 */
public class M {

	public static final String META_INF_VORTEX_CLASSES_METADATA_JSON = "/META-INF/vortex/classes-metadata.json";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	public static Set<String> loadAllClasses() {
		try (InputStream is = M.class.getResourceAsStream(META_INF_VORTEX_CLASSES_METADATA_JSON)) {
			if (is == null) {
				return Set.of();
			}
			return MAPPER.readValue(is, new TypeReference<Set<String>>() {});
		} catch (final Exception e) {
			throw new RuntimeException("Failed to load classes metadata", e);
		}
	}
}
