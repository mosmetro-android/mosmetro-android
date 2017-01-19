package pw.thedrhax.mosmetro;

import javax.lang.model.element.ExecutableElement;

/** This is a class which stores information about annotated method. */
final class AnnotatedMethod {
    /** The main annotated element. */
    private final ExecutableElement typeElement;

    /** The name of the annotated method. */
    private final String annotatedMethodName;

    static AnnotatedMethod create(ExecutableElement typeElement) {
        return new AnnotatedMethod(typeElement);
    }

    private AnnotatedMethod(ExecutableElement typeElement) {
        this.typeElement = typeElement;
        this.annotatedMethodName = typeElement.getSimpleName().toString();
    }

    public ExecutableElement typeElement() {
        return typeElement;
    }

    public String methodName() {
        return annotatedMethodName;
    }
}
