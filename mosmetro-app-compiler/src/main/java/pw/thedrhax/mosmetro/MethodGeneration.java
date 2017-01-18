package pw.thedrhax.mosmetro;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import java.util.Map;

import javax.lang.model.element.Name;

import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static pw.thedrhax.mosmetro.MethodValidator.CLIENT_CLASS_PARAMETER;
import static pw.thedrhax.mosmetro.MethodValidator.PROVIDER_CLASS_PARAMETER;

final class MethodGeneration {
    private static final String METHOD_NAME = "check";
    private static final String CLIENT_PARAMETER_NAME = "client";

    private static final ClassName CONTEXT_CLASS_PARAMETER = ClassName.get("android.content", "Context");
    private static final String CONTEXT_PARAMETER_NAME = "context";

    private static final ClassName UNKNOWN_CLASS_PROVIDER_PARAMETER = ClassName.get("pw.thedrhax.mosmetro.authenticator.providers", "Unknown");
    private static final ClassName NON_NULL_ANNOTATION = ClassName.get("android.support.annotation", "NonNull");

    /**
     * Generate the main method that will choose the right {@code Provider}.
     *
     * @return the ready-to-write method.
     */
    static MethodSpec makeMethod(Map<String, AnnotatedMethod> matchableMap) {
        // Generate the resulting method that will cover the decision.
        MethodSpec.Builder builder = MethodSpec.methodBuilder(METHOD_NAME);

        // Add javadoc to the method.
        builder.addJavadoc(
            CodeBlock.of("Use this method to determine which {@link $1T} to use for authentication.\n\n" +
                    "@param $2L is used for initializing a concrete {@link $1T}.\n" +
                    "@param $3L is for passing the {@link $4T} to {@link $1T} and determining if this client is OK to be used.\n",
                PROVIDER_CLASS_PARAMETER,
                CONTEXT_PARAMETER_NAME,
                CLIENT_PARAMETER_NAME,
                CLIENT_CLASS_PARAMETER
            )
        );

        // Accessible method all over the place.
        builder.addModifiers(PUBLIC, STATIC);

        // Add @NonNull annotation.
        builder.addAnnotation(NON_NULL_ANNOTATION);

        // Return an initialized Provider descendant.
        builder.returns(PROVIDER_CLASS_PARAMETER);

        // This parameter is for initializing Provider descendant.
        builder.addParameter(ParameterSpec.builder(CONTEXT_CLASS_PARAMETER, CONTEXT_PARAMETER_NAME)
            .addAnnotation(NON_NULL_ANNOTATION)
            .build());

        // This parameter is for supplying it to the 'match()' method.
        builder.addParameter(ParameterSpec.builder(CLIENT_CLASS_PARAMETER, CLIENT_PARAMETER_NAME)
            .addAnnotation(NON_NULL_ANNOTATION)
            .build());

        boolean first = true;
        String controlFlow;
        // Iterate over the saved map of all annotated methods.
        for (Map.Entry<String, AnnotatedMethod> entry : matchableMap.entrySet()) {
            controlFlow = "if ($T.$L($L))";
            TypeName typeName = ClassName.get(entry.getValue().typeElement().getEnclosingElement().asType());
            Name simpleName = entry.getValue().typeElement().getSimpleName();
            if (first) {
                builder.beginControlFlow(controlFlow, typeName, simpleName, CLIENT_PARAMETER_NAME);
            } else {
                controlFlow = "else ".concat(controlFlow);
                builder.nextControlFlow(controlFlow, typeName, simpleName, CLIENT_PARAMETER_NAME);
            }
            builder.addStatement("return new $T($L)", typeName, CONTEXT_PARAMETER_NAME);

            first = false;
        }
        builder.endControlFlow();
        builder.addStatement("return new $T($L)", UNKNOWN_CLASS_PROVIDER_PARAMETER, CONTEXT_PARAMETER_NAME);

        return builder.build();
    }
}
