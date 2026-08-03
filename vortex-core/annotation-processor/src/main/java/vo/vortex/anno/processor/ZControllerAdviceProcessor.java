//package vo.vortex.anno.processor;
//
//import java.io.PrintWriter;
//import java.time.LocalDateTime;
//import java.util.Set;
//
//import javax.annotation.processing.AbstractProcessor;
//import javax.annotation.processing.Filer;
//import javax.annotation.processing.RoundEnvironment;
//import javax.annotation.processing.SupportedAnnotationTypes;
//import javax.annotation.processing.SupportedSourceVersion;
//import javax.lang.model.SourceVersion;
//import javax.lang.model.element.Element;
//import javax.lang.model.element.TypeElement;
//import javax.tools.Diagnostic;
//import javax.tools.JavaFileObject;
//
//import vo.vortex.anno.ZControllerAdvice;
//
///**
// *
// *
// * @author zhangzhen
// * @date 2026年8月2日 02:45:45
// */
//@SupportedAnnotationTypes({ "vo.vortex.anno.ZControllerAdvice" })
//@SupportedSourceVersion(SourceVersion.RELEASE_25)
//	public class ZControllerAdviceProcessor extends AbstractProcessor {
//
//	private static boolean generated = false;
//	public static final String PACKAGE_NAME = "vo.vortex.apt.generated";
//	public static final String CLASS_NAME = "ZControllerAdviceRegistry";
//
//	@Override
//	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
//		if (generated) {
//			return true;
//		}
//
//		System.out.println(LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
//				+ "ZControllerAdviceProcessor.process()");
//
//	    final Set<? extends Element> typeSet = roundEnv.getElementsAnnotatedWith(ZControllerAdvice.class);
//		System.out.println("typeCASet.size = " + typeSet.size());
//		for (final Element element : typeSet) {
//			System.out.println(element);
//			System.out.println(element.toString());
//
//		}
//		System.out.println("typeCASet.size = " + typeSet.size());
//
//	     try {
//	         final String packageName = PACKAGE_NAME;
//	         final String className = CLASS_NAME;
//
//	         final Filer filer = this.processingEnv.getFiler();
//	         final JavaFileObject sourceFile = filer.createSourceFile(packageName + "." + className);
//	         try (PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
//	             out.println("package " + packageName + ";");
//	             out.println();
//	             out.println("import java.util.Set;");
//	             out.println("import java.util.LinkedHashSet;");
//	             out.println();
//	             out.println("public class " + className + " {");
//	             out.println();
//	             out.println("    public static Set<String> getAllClass() {");
//	             out.println("        Set<String> set = new LinkedHashSet<>();");
//
//	             for (final Element e : typeSet) {
//	                 final String fullName = e.toString();
//	                 out.println("        set.add(\"" + fullName + "\");");
//	             }
//
//	             out.println("        return set;");
//	             out.println("    }");
//	             out.println("}");
//	         }
//		} catch (final Exception e) {
//			this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
//					"Failed to generate " + CLASS_NAME + ": " + e.getMessage());
//		}
//
//	    generated = true;
//
//		return true;
//	}
//
//}
