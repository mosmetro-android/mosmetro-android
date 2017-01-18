package pw.thedrhax.mosmetro;

import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.tools.Diagnostic.Kind.ERROR;
import static pw.thedrhax.mosmetro.MethodValidator.CLIENT_CLASS_PARAMETER;
import static pw.thedrhax.mosmetro.MethodValidator.PROVIDER_CLASS_PARAMETER;

@AutoService(Processor.class)
public final class MatchableProcessor extends AbstractProcessor {
    private static final String GENERATED_CLASS_NAME = "ProviderChooser";
    private static final ClassName MOSMETRO_V1_PROVIDER_CLASS_PARAMETER = ClassName.get("pw.thedrhax.mosmetro.authenticator.providers", "MosMetroV1");

    private Messager messager;

    @Override public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        // If you encounter an error during processing, do not throw an exception.
        // Instead use the Messager class to display errors in the build results.
        // Clicking these errors will bring you to the line that caused the error.
        messager = env.getMessager();
    }

    @Override public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Matchable.class.getCanonicalName());
        return annotations;
    }

    @Override public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        // We store every name of the class as a key,
        // and every annotated method as a AnnotatedMethod.
        Map<String, AnnotatedMethod> matchableMap = findAndParseTargets(env);

        if (matchableMap.size() > 0) {
            try {
                makeFile(matchableMap);
            } catch (IOException e) {
                logError(e);
            }
        }

        return true;
    }

    /** Loop every annotated method which are annotated with Matchable.class. */
    private Map<String, AnnotatedMethod> findAndParseTargets(RoundEnvironment env) {
        Map<String, AnnotatedMethod> matchableMap = new LinkedHashMap<>();

        for (Element element : env.getElementsAnnotatedWith(Matchable.class)) {
            if (!SuperficialValidation.validateElement(element)) continue;
            try {
                processAnnotatedElement(matchableMap, element);
            } catch (Exception e) {
                logError(e);
            }
        }

        return matchableMap;
    }

    /** Process annotated element. */
    private void processAnnotatedElement(Map<String, AnnotatedMethod> matchableMap, Element element) {
        boolean hasError = false;

        // ExecutableElement represents a method, constructor, or initializer (static or instance)
        // of a class or interface, including annotation type elements.
        // We can assume this because Matchable.class has ElementType.METHOD in declaration.
        ExecutableElement annotatedMethod = (ExecutableElement) element;

        // Check that this method is valid to process.
        hasError |= !MethodValidator.isValidMethod(messager, annotatedMethod);

        // Get the class name where this method is enclosed.
        String processedClass = element.getEnclosingElement().getSimpleName().toString();

        // Check if we have already processed this class before.
        // If we have already processed this class, then there is an error.
        hasError |= matchableMap.get(processedClass) != null;

        if (hasError) {
            return;
        }

        // Put the annotated method to the map.
        matchableMap.put(processedClass, AnnotatedMethod.create(annotatedMethod));
    }

    /**
     * Generate new classes near the original class where the annotation {@link Matchable}
     * was used.
     *
     * @param matchableMap all saved classes and annotated methods.
     * @throws IOException if something wrong with the writing file.
     */
    private void makeFile(Map<String, AnnotatedMethod> matchableMap) throws IOException {
        // Build class 'ProviderChooser'.
        TypeSpec.Builder builder = TypeSpec.classBuilder(GENERATED_CLASS_NAME)
            .addJavadoc(
                CodeBlock.of(
                    "This is a generated class for determining which {@link $1T} to use.<br>\n" +
                        "Annotate the method in one of the {@link $1T} implementations to include it here.<br>\n" +
                        "Example: {@link $2T#match($3T)}.\n",
                    PROVIDER_CLASS_PARAMETER,
                    MOSMETRO_V1_PROVIDER_CLASS_PARAMETER,
                    CLIENT_CLASS_PARAMETER
                )
            )
            .addModifiers(PUBLIC, FINAL);

        // Build method.
        builder.addMethod(MethodGeneration.makeMethod(matchableMap));

        // Build and save file to disk.
        TypeSpec typeSpec = builder.build();
        JavaFile javaFile = JavaFile.builder(PROVIDER_CLASS_PARAMETER.packageName(), typeSpec)
            .addFileComment("Generated code from Matchable Processor. Do not modify!")
            .build();
        javaFile.writeTo(processingEnv.getFiler());
    }

    private void logError(Exception e) {
        messager.printMessage(ERROR, "Couldn't generate class with exception: ".concat(e.getMessage()));
    }
}
