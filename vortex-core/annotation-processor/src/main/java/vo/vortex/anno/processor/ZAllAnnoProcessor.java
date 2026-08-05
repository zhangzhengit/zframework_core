package vo.vortex.anno.processor;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import vo.vortex.anno.ZAOP;
import vo.vortex.anno.ZCacheEvict;
import vo.vortex.anno.ZCachePut;
import vo.vortex.anno.ZCacheable;
import vo.vortex.anno.ZComponent;
import vo.vortex.anno.ZConfiguration;
import vo.vortex.anno.ZConfigurationProperties;
import vo.vortex.anno.ZController;
import vo.vortex.anno.ZControllerAdvice;
import vo.vortex.anno.ZEndsWith;
import vo.vortex.anno.ZLength;
import vo.vortex.anno.ZMax;
import vo.vortex.anno.ZMin;
import vo.vortex.anno.ZNotEmtpy;
import vo.vortex.anno.ZNotNull;
import vo.vortex.anno.ZPositive;
import vo.vortex.anno.ZRestController;
import vo.vortex.anno.ZService;
import vo.vortex.anno.ZStartWith;
import vo.vortex.anno.ZUnique;

/**
 *
 *
 * @author zhangzhen
 * @date 2026年8月2日 02:45:45
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class ZAllAnnoProcessor extends AbstractProcessor {

	public static final String Z_ALL_BEAN_REGISTRY = "ZAllBeanRegistry";
	public static final String PACKAGE_NAME = "vo.vortex.generated.apt";
	public static final String ZCacheRegistry = "ZCacheRegistry";
	public static final String ZControllerAdviceRegistry = "ZControllerAdviceRegistry";
	public static final String ZRestControllerRegistry = "ZRestControllerRegistry";
	private static boolean generated = false;

	@Override
	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
		if (generated) {
			return true;
		}

		System.out.println(
				LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "ZAllAnnoProcessor.process()");

		this.gAllClass(roundEnv);

//		this.gZCache(roundEnv);
//
//		this.gZControllerAdvice(roundEnv);
//
//
//		this.gZConfiguration(roundEnv);
//
//		this.gZAOP(roundEnv);
//
//
//		this.gZComponent(roundEnv);
//
//		this.gZService(roundEnv);
//
//		this.gZController(roundEnv);
//
//		this.gZConfigurationProperties(roundEnv);
//
//		this.gZValidator(roundEnv);


		generated = true;
		return true;
	}

	private void gZConfigurationProperties(final RoundEnvironment roundEnv) {
		final Set<? extends Element> configurationPropertiesSet = roundEnv.getElementsAnnotatedWith(ZConfigurationProperties.class);
		this.gJson(configurationPropertiesSet, "ZConfigurationPropertiesRegistry");
	}

	private void gZConfiguration(final RoundEnvironment roundEnv) {
		final Set<? extends Element> configurationSet = roundEnv.getElementsAnnotatedWith(ZConfiguration.class);
		this.gJson(configurationSet, "ZConfigurationRegistry");
	}

	private void gZAOP(final RoundEnvironment roundEnv) {
		final Set<? extends Element> zaopSet = roundEnv.getElementsAnnotatedWith(ZAOP.class);
		this.gJson(zaopSet, "ZAOPRegistry");
	}

	private void gZService(final RoundEnvironment roundEnv) {
		final Set<? extends Element> serviceSet = roundEnv.getElementsAnnotatedWith(ZService.class);
		this.gJson(serviceSet, "ZServiceRegistry");
	}

	private void gZComponent(final RoundEnvironment roundEnv) {
		final Set<? extends Element> componentSet = roundEnv.getElementsAnnotatedWith(ZComponent.class);
		this.gJson(componentSet, "ZComponentRegistry");
	}

	private void gAllClass(final RoundEnvironment roundEnv) {
		final Set<? extends Element> rootElements = roundEnv.getRootElements();
	    final Set<Element> csset = rootElements.stream().filter(e -> e.getKind() == ElementKind.CLASS)
	    .collect(Collectors.toSet());
	    this.gJson(csset, Z_ALL_BEAN_REGISTRY);
	}

	private void gZValidator(final RoundEnvironment roundEnv) {
		final Set<Element> typeSet = roundEnv.getElementsAnnotatedWith(ZNotNull.class).stream().map(Element::getEnclosingElement).collect(Collectors.toSet());
		final Set<Element> t2 = roundEnv.getElementsAnnotatedWith(ZNotEmtpy.class).stream().map(Element::getEnclosingElement).collect(Collectors.toSet());
		final Set<Element> t3 = roundEnv.getElementsAnnotatedWith(ZStartWith.class).stream().map(Element::getEnclosingElement).collect(Collectors.toSet());
		final Set<Element> t4 = roundEnv.getElementsAnnotatedWith(ZEndsWith.class).stream().map(Element::getEnclosingElement).collect(Collectors.toSet());
		final Set<Element> t5 = roundEnv.getElementsAnnotatedWith(ZLength.class).stream().map(Element::getEnclosingElement).collect(Collectors.toSet());
		final Set<Element> t6 = roundEnv.getElementsAnnotatedWith(ZMin.class).stream().map(Element::getEnclosingElement).collect(Collectors.toSet());
		final Set<Element> t7 = roundEnv.getElementsAnnotatedWith(ZMax.class).stream().map(Element::getEnclosingElement).collect(Collectors.toSet());
		final Set<Element> t8 = roundEnv.getElementsAnnotatedWith(ZUnique.class).stream().map(Element::getEnclosingElement).collect(Collectors.toSet());
		final Set<Element> t9 = roundEnv.getElementsAnnotatedWith(ZPositive.class).stream().map(Element::getEnclosingElement).collect(Collectors.toSet());

		typeSet.addAll(t2);
		typeSet.addAll(t3);
		typeSet.addAll(t4);
		typeSet.addAll(t5);
		typeSet.addAll(t6);
		typeSet.addAll(t7);
		typeSet.addAll(t8);
		typeSet.addAll(t9);

		this.gJson(typeSet, "ZValidatorRegistry");
	}

	private void gZController(final RoundEnvironment roundEnv) {
		final Set<? extends Element> restControllerSet = roundEnv.getElementsAnnotatedWith(ZRestController.class);
		this.gJson(restControllerSet, "ZRestControllerRegistry");

		final Set<? extends Element> controllerSet = roundEnv.getElementsAnnotatedWith(ZController.class);
		this.gJson(controllerSet, "ZControllerRegistry");
	}

	private void gZControllerAdvice(final RoundEnvironment roundEnv) {
		final Set<? extends Element> controllerAdviceSet = roundEnv.getElementsAnnotatedWith(ZControllerAdvice.class);
		this.gJson(controllerAdviceSet, "ZControllerAdviceRegistry");
	}

	private void gZCache(final RoundEnvironment roundEnv) {
		final Set<Element> cacheESet = roundEnv.getElementsAnnotatedWith(ZCacheable.class).stream()
				.map(Element::getEnclosingElement).collect(Collectors.toSet());
		cacheESet.addAll(roundEnv.getElementsAnnotatedWith(ZCachePut.class).stream().map(Element::getEnclosingElement)
				.collect(Collectors.toSet()));
		cacheESet.addAll(roundEnv.getElementsAnnotatedWith(ZCacheEvict.class).stream().map(Element::getEnclosingElement)
				.collect(Collectors.toSet()));
		this.gJson(cacheESet, "ZCacheRegistry");
	}

	private void gJson(final Set<? extends Element> elementSet, final String className) {
	    final List<String> classNames = elementSet.stream()
	            .map(Element::toString)
	            .sorted()
	            .collect(Collectors.toList());

	    final String json = classNames.stream()
	            .map(s -> "\"" + s + "\"")
	            .collect(Collectors.joining(", ", "[", "]"));

	    try {
	        final Filer filer = this.processingEnv.getFiler();
	        final FileObject fileObject = filer.createResource(
	                StandardLocation.CLASS_OUTPUT,
	                "",
	                "META-INF/vortex/registry/" + className + ".json");
	        try (Writer writer = fileObject.openWriter()) {
	            writer.write(json);
	        }
	        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
	                "Generated registry JSON: META-INF/vortex/registry/" + className + ".json");
	    } catch (final IOException e) {
	        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
	                "Failed to generate registry JSON: " + e.getMessage());
	    }
	}

}
