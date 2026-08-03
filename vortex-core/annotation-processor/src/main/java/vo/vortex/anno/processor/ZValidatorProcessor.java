//package vo.vortex.anno.processor;
//
//import java.io.PrintWriter;
//import java.time.LocalDateTime;
//import java.util.Set;
//import java.util.stream.Collector;
//import java.util.stream.Collectors;
//
//import javax.annotation.processing.AbstractProcessor;
//import javax.annotation.processing.Filer;
//import javax.annotation.processing.RoundEnvironment;
//import javax.annotation.processing.SupportedAnnotationTypes;
//import javax.annotation.processing.SupportedSourceVersion;
//import javax.lang.model.SourceVersion;
//import javax.lang.model.element.Element;
//import javax.lang.model.element.ElementKind;
//import javax.lang.model.element.TypeElement;
//import javax.tools.Diagnostic;
//import javax.tools.JavaFileObject;
//
//import vo.vortex.anno.ZEndsWith;
//import vo.vortex.anno.ZLength;
//import vo.vortex.anno.ZMax;
//import vo.vortex.anno.ZMin;
//import vo.vortex.anno.ZNotEmtpy;
//import vo.vortex.anno.ZNotNull;
//import vo.vortex.anno.ZPositive;
//import vo.vortex.anno.ZStartWith;
//import vo.vortex.anno.ZUnique;
//
///**
// *
// *
// * @author zhangzhen
// * @date 2026年8月2日 02:45:45
// */
//@SupportedAnnotationTypes({
//		"vo.vortex.anno.ZNotNull"
//	,
//		"vo.vortex.anno.ZNotEmtpy"
//	,
//		"vo.vortex.anno.ZStartWith",
//		"vo.vortex.anno.ZEndsWith",
//		"vo.vortex.anno.ZLength",
//		"vo.vortex.anno.ZMin",
//		"vo.vortex.anno.ZMax",
//		"vo.vortex.anno.ZUnique",
//		"vo.vortex.anno.ZPositive"
//	})
//@SupportedSourceVersion(SourceVersion.RELEASE_25)
//public class ZValidatorProcessor  extends AbstractProcessor {
//
//	 private static boolean generated = false;
//	public static final String PACKAGE_NAME = "vo.vortex.apt.generated";
//	public static final String CLASS_NAME = "ZValidatorRegistry";
//	public static final String CLASS_NAME_ALL_BEAN = "ZAllBeanRegistry";
//
//	@Override
//	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
//
//		if(generated) {
//			return true;
//		}
//		System.out.println(
//				LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "ZValidatorProcessor.process()");
//
//		//		System.out.println("notNullSet.size = " + notNullSet.size());
//
//		final Set<Element> typeSet = roundEnv.getElementsAnnotatedWith(ZNotNull.class).stream().map(Element::getEnclosingElement).collect(Collectors.toSet());
//		final Set<Element> t2 = roundEnv.getElementsAnnotatedWith(ZNotEmtpy.class).stream().map(Element::getEnclosingElement).collect(Collectors.toSet());
//		final Set<Element> t3 = roundEnv.getElementsAnnotatedWith(ZStartWith.class).stream().map(Element::getEnclosingElement).collect(Collectors.toSet());
//		final Set<Element> t4 = roundEnv.getElementsAnnotatedWith(ZEndsWith.class).stream().map(Element::getEnclosingElement).collect(Collectors.toSet());
//		final Set<Element> t5 = roundEnv.getElementsAnnotatedWith(ZLength.class).stream().map(Element::getEnclosingElement).collect(Collectors.toSet());
//		final Set<Element> t6 = roundEnv.getElementsAnnotatedWith(ZMin.class).stream().map(Element::getEnclosingElement).collect(Collectors.toSet());
//		final Set<Element> t7 = roundEnv.getElementsAnnotatedWith(ZMax.class).stream().map(Element::getEnclosingElement).collect(Collectors.toSet());
//		final Set<Element> t8 = roundEnv.getElementsAnnotatedWith(ZUnique.class).stream().map(Element::getEnclosingElement).collect(Collectors.toSet());
//		final Set<Element> t9 = roundEnv.getElementsAnnotatedWith(ZPositive.class).stream().map(Element::getEnclosingElement).collect(Collectors.toSet());
//
//		typeSet.addAll(t2);
//		typeSet.addAll(t3);
//		typeSet.addAll(t4);
//		typeSet.addAll(t5);
//		typeSet.addAll(t6);
//		typeSet.addAll(t7);
//		typeSet.addAll(t8);
//		typeSet.addAll(t9);
//
//		System.out.println("typeSet.size = " + typeSet.size());
//		for (final Element element : typeSet) {
//			System.out.println(element);
//			System.out.println(element.toString());
//
//		}
//		System.out.println("typeSet.size = " + typeSet.size());
//
//	    this.g1(typeSet, CLASS_NAME);
//
//	    final Set<? extends Element> rootElements = roundEnv.getRootElements();
//	    final Set<Element> csset = rootElements.stream().filter(e -> e.getKind() == ElementKind.CLASS)
//	    .collect(Collectors.toSet());
//	    this.g1(csset, CLASS_NAME_ALL_BEAN);
//
//	     generated = true; // 标记已生成
//		return true;
//	}
//
//	private void g1(final Set<Element> typeSet, final String className) {
//		try {
//	         final String packageName = PACKAGE_NAME;
//
//	         final Filer filer = this.processingEnv.getFiler();
//	         final JavaFileObject sourceFile = filer.createSourceFile(packageName + "." + className);
//	         try (PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
//	             out.println("package " + packageName + ";");
//	             out.println();
//	             out.println("import vo.vortex.anno.ZNotNull;");
//	             out.println("import java.util.Set;");
//	             out.println("import java.util.LinkedHashSet;");
//	             out.println();
//	             out.println("public class " + className + " {");
//	             out.println();
//	             out.println(" @ZNotNull String name;");
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
//	}
//
//}
