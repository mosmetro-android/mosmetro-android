package pw.thedrhax.mosmetro;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;

/** Simple class to validate the annotated method. */
final class MethodValidator {
    private static final String ANNOTATION = "@" + Matchable.class.getSimpleName();

    static final ClassName CLIENT_CLASS_PARAMETER = ClassName.get("pw.thedrhax.mosmetro.httpclient", "Client");
    static final ClassName PROVIDER_CLASS_PARAMETER = ClassName.get("pw.thedrhax.mosmetro.authenticator", "Provider");

    /** Determine if current method is valid for our needs. */
    static boolean isValidMethod(Messager messager, ExecutableElement executableElement) {
        // Check for public, static modifiers.
        if (!isPublic(executableElement) && !isStatic(executableElement) && !hasNoModifier(executableElement)) {
            String message = String.format(
                "Methods annotated with %s must be public and static.",
                ANNOTATION
            );
            messager.printMessage(ERROR, message, executableElement);
            return false;
        }

        // Check that the method has one and only one parameter pw.thedrhax.mosmetro.httpclient.Client.
        if (hasNoParameters(executableElement) || !hasOneParameter(executableElement) || !hasFirstClientParameter(executableElement)) {
            String message = String.format(
                "Methods annotated with %1$s must have %2$s parameter 'client' because it is required by design.",
                ANNOTATION,
                CLIENT_CLASS_PARAMETER
            );
            messager.printMessage(ERROR, message, executableElement);
            return false;
        }

        // Check that method is included in the class which extends Provider in some way.
        if (!isInProviderDescendant(executableElement)) {
            String message = String.format("Methods annotated with %1$s must extend %2$s.",
                ANNOTATION,
                PROVIDER_CLASS_PARAMETER
            );
            messager.printMessage(ERROR, message, executableElement);
            return false;
        }

        return true;
    }

    private static boolean isPublic(ExecutableElement executableElement) {
        return executableElement.getModifiers().contains(PUBLIC);
    }

    private static boolean isStatic(ExecutableElement executableElement) {
        return executableElement.getModifiers().contains(STATIC);
    }

    private static boolean hasNoModifier(ExecutableElement executableElement) {
        return executableElement.getModifiers().isEmpty();
    }

    private static boolean hasNoParameters(ExecutableElement executableElement) {
        return executableElement.getParameters().size() == 0;
    }

    private static boolean hasOneParameter(ExecutableElement executableElement) {
        return executableElement.getParameters().size() == 1;
    }

    private static boolean hasClientParameter(ExecutableElement executableElement) {
        boolean hasClientParam = false;
        for (VariableElement variableElement : executableElement.getParameters()) {
            if (TypeName.get(variableElement.asType()).equals(CLIENT_CLASS_PARAMETER)) {
                hasClientParam = true;
                break;
            }
        }
        return hasClientParam;
    }

    private static boolean isInProviderDescendant(ExecutableElement executableElement) {
        return hasSuperClass(executableElement, PROVIDER_CLASS_PARAMETER.reflectionName());
    }

    private static boolean hasFirstClientParameter(ExecutableElement executableElement) {
        return TypeName.get(executableElement.getParameters().get(0).asType()).equals(CLIENT_CLASS_PARAMETER);
    }

    /**
     * Determine if current method is in class that extends a specified class somewhere in the hierarchy.
     *
     * @param executableElement  the method to process.
     * @param superClassToSearch the superclass to search in the hierarchy.
     * @return {@code true} if such class exists in the hierarchy, {@code false} otherwise.
     */
    private static boolean hasSuperClass(ExecutableElement executableElement, String superClassToSearch) {
        Element element = executableElement.getEnclosingElement();

        TypeElement superClass = MethodValidator.getSuperClassElement(element);

        // Loop until we get superClassToSearch class. Otherwise return false.
        while (superClass != null) {
            if (superClassToSearch.equals(superClass.getQualifiedName().toString())) {
                return true;
            }
            superClass = MethodValidator.getSuperClassElement(superClass);
        }
        return false;
    }

    /**
     * Retrieves the super class of the given {@link Element}.
     * Returns null if {@link Element} represents {@link Object},
     * or something other than {@link ElementKind#CLASS}.
     *
     * @param element target {@link Element}.
     * @return {@link Element} of its super class.
     */
    private static TypeElement getSuperClassElement(Element element) {
        if (element.getKind() != ElementKind.CLASS) {
            return null;
        }
        TypeMirror superclass = ((TypeElement) element).getSuperclass();
        if (superclass.getKind() == TypeKind.NONE) {
            return null;
        }
        DeclaredType kind = (DeclaredType) superclass;
        return (TypeElement) kind.asElement();
    }
}
