package vo.vortex.g;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import vo.vortex.anno.processor.ZAllAnnoProcessor;
import vo.vortex.common.CU;

/**
 * 读取APT读取到的Class信息
 *
 * @author zhangzhen
 * @date 2026年8月2日 11:45:14
 */
public class APT {

	private static final String path = "/META-INF/vortex/registry/" + ZAllAnnoProcessor.Z_ALL_BEAN_REGISTRY
			+ ".json";

	private static final String VO_VORTEX = "vo.vortex";

	private static Set<Class<?>> zsSet;
	private static Set<String> cnset;

	 static {
	        Set<String> loadedNames = new HashSet<>();
	        try (InputStream is = G.class
	                .getResourceAsStream(path)) {
	            if (is != null) {
	                final ObjectMapper mapper = new ObjectMapper();
	                // 假设 JSON 格式为 ["class1", "class2", ...]
	                loadedNames = mapper.readValue(is, new TypeReference<Set<String>>() {});
	            }
	        } catch (final Exception e) {
	        	System.out.println("APTread-path 异常");
	            // 日志记录
	        	e.printStackTrace();

	        }
//	        CLASS_NAMES = Collections.unmodifiableSet(loadedNames);

//	        System.out.println("APTloadedNames.size = " + loadedNames.size());
//	        for (final String string : loadedNames) {
//				System.out.println(string);
//			}
//	        System.out.println("APTloadedNames.size = " + loadedNames.size());
	        cnset = loadedNames;
	    }

	public static Set<Class<?>> getAllClass() {
		if (zsSet == null) {
			final Set<String> cnset = getAllClassName();
			zsSet = cnset.stream()
//					.filter(cn -> cn.startsWith(VO_VORTEX))
					.map(G::load)
					.filter(c -> c!=null)
					.collect(Collectors.toSet());

		}
		return zsSet;
	}

	public static Class<?> load(final String className) {
		try {
			return Class.forName(className,false,Thread.currentThread().getContextClassLoader());
		} catch (final ExceptionInInitializerError | ClassNotFoundException  e) {
			return null;
		}
	}

	public static Set<Class<?>> load(final Set<String> classNameSet) {
		if (CU.isEmpty(classNameSet)) {
			return Collections.emptySet();
		}
		return classNameSet.stream().map(G::load).collect(Collectors.toSet());
	}

	static Set<String> getAllClassName() {

		// 2
		return cnset;

		// 1
//		try {
//			final Class<?> clazz = Class.forName("vo.vortex.generated.IndexHolder");
//			final java.lang.reflect.Method method = clazz.getMethod("getClassNames");
//			final Set<String> classNames = (Set<String>) method.invoke(null);
//			return classNames;
//		} catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
//				| InvocationTargetException e) {
//			e.printStackTrace();
//		}
//		return Collections.emptySet();
	}

}
