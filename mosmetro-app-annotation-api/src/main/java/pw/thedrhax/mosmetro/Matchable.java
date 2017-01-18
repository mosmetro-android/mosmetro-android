package pw.thedrhax.mosmetro;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Create a method which will be included in possible {@code Provider} choosing.
 */
@Retention(CLASS) @Target(METHOD)
public @interface Matchable {
}
