package pw.thedrhax.mosmetro;

import com.google.testing.compile.JavaFileObjects;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static pw.thedrhax.mosmetro.MethodValidator.ANNOTATION;
import static pw.thedrhax.mosmetro.MethodValidator.CLIENT_CLASS_PARAMETER;
import static pw.thedrhax.mosmetro.MethodValidator.PROVIDER_CLASS_PARAMETER;

public class MatchableProcessorTest {

    private JavaFileObject context;
    private JavaFileObject nonNull;

    private JavaFileObject concreteProvider;
    private JavaFileObject concreteProvider2;
    private JavaFileObject concreteProvider3;
    private JavaFileObject concreteProvider4;

    private JavaFileObject unknown;
    private JavaFileObject provider;
    private JavaFileObject client;

    @Before public void setup() {
        context = JavaFileObjects.forSourceString("android.content.Context", "" +
            "package android.content;\n" +
            "public abstract class Context {\n" +
            "}\n"
        );
        nonNull = JavaFileObjects.forSourceString("android.support.annotation.NonNull", "" +
            "package android.support.annotation;\n" +
            "import java.lang.annotation.Documented;\n" +
            "import java.lang.annotation.Retention;\n" +
            "import java.lang.annotation.Target;\n" +
            "import static java.lang.annotation.ElementType.ANNOTATION_TYPE;\n" +
            "import static java.lang.annotation.ElementType.FIELD;\n" +
            "import static java.lang.annotation.ElementType.METHOD;\n" +
            "import static java.lang.annotation.ElementType.PACKAGE;\n" +
            "import static java.lang.annotation.ElementType.PARAMETER;\n" +
            "import static java.lang.annotation.RetentionPolicy.CLASS;\n" +
            "@Documented\n" +
            "@Retention(CLASS)\n" +
            "@Target({METHOD, PARAMETER, FIELD, ANNOTATION_TYPE, PACKAGE})\n" +
            "public @interface NonNull {\n" +
            "}\n"
        );
        concreteProvider = JavaFileObjects.forSourceString("pw.thedrhax.mosmetro.authenticator.providers.ConcreteProvider", "" +
            "package pw.thedrhax.mosmetro.authenticator.providers;\n" +
            "import android.content.Context;\n" +
            "import java.util.Random;\n" +
            "import pw.thedrhax.mosmetro.Matchable;\n" +
            "import pw.thedrhax.mosmetro.authenticator.Provider;\n" +
            "import pw.thedrhax.mosmetro.httpclient.Client;\n" +
            "public class ConcreteProvider extends Provider {\n" +
            "    private static final Random random = new Random();\n" +
            "    public ConcreteProvider(Context context) {\n" +
            "        super(context);\n" +
            "    }\n" +
            "    @Matchable public static boolean match(Client client) {\n" +
            "        return random.nextBoolean();\n" +
            "    }\n" +
            "}\n"
        );
        concreteProvider2 = JavaFileObjects.forSourceString("pw.thedrhax.mosmetro.authenticator.providers.ConcreteProvider2", "" +
            "package pw.thedrhax.mosmetro.authenticator.providers;\n" +
            "import android.content.Context;\n" +
            "import java.util.Random;\n" +
            "import pw.thedrhax.mosmetro.Matchable;\n" +
            "import pw.thedrhax.mosmetro.authenticator.Provider;\n" +
            "import pw.thedrhax.mosmetro.httpclient.Client;\n" +
            "public class ConcreteProvider2 extends Provider {\n" +
            "    private static final Random random = new Random();\n" +
            "    public ConcreteProvider2(Context context) {\n" +
            "        super(context);\n" +
            "    }\n" +
            "    @Matchable public static boolean match(Client client) {\n" +
            "        return random.nextBoolean();\n" +
            "    }\n" +
            "}\n"
        );
        concreteProvider3 = JavaFileObjects.forSourceString("pw.thedrhax.mosmetro.authenticator.providers.ConcreteProvider3", "" +
            "package pw.thedrhax.mosmetro.authenticator.providers;\n" +
            "import android.content.Context;\n" +
            "import java.util.Random;\n" +
            "import pw.thedrhax.mosmetro.Matchable;\n" +
            "import pw.thedrhax.mosmetro.authenticator.Provider;\n" +
            "import pw.thedrhax.mosmetro.httpclient.Client;\n" +
            "public class ConcreteProvider3 extends Provider {\n" +
            "    private static final Random random = new Random();\n" +
            "    public ConcreteProvider3(Context context) {\n" +
            "        super(context);\n" +
            "    }\n" +
            "    @Matchable public static boolean match(Client client) {\n" +
            "        return random.nextBoolean();\n" +
            "    }\n" +
            "}\n"
        );
        concreteProvider4 = JavaFileObjects.forSourceString("pw.thedrhax.mosmetro.authenticator.providers.ConcreteProvider4", "" +
            "package pw.thedrhax.mosmetro.authenticator.providers;\n" +
            "import android.content.Context;\n" +
            "import java.util.Random;\n" +
            "import pw.thedrhax.mosmetro.Matchable;\n" +
            "import pw.thedrhax.mosmetro.authenticator.Provider;\n" +
            "import pw.thedrhax.mosmetro.httpclient.Client;\n" +
            "public class ConcreteProvider4 extends Provider {\n" +
            "    private static final Random random = new Random();\n" +
            "    public ConcreteProvider4(Context context) {\n" +
            "        super(context);\n" +
            "    }\n" +
            "    @Matchable public static boolean match(Client client) {\n" +
            "        return random.nextBoolean();\n" +
            "    }\n" +
            "}\n"
        );
        unknown = JavaFileObjects.forSourceString("pw.thedrhax.mosmetro.authenticator.providers.Unknown", "" +
            "package pw.thedrhax.mosmetro.authenticator.providers;\n" +
            "import android.content.Context;\n" +
            "import java.util.Random;\n" +
            "import pw.thedrhax.mosmetro.authenticator.Provider;\n" +
            "public class Unknown extends Provider {\n" +
            "    private static final Random random = new Random();\n" +
            "    public Unknown(Context context) {\n" +
            "        super(context);\n" +
            "    }\n" +
            "}\n"
        );
        provider = JavaFileObjects.forSourceString("pw.thedrhax.mosmetro.authenticator.Provider", "" +
            "package pw.thedrhax.mosmetro.authenticator;\n" +
            "import android.content.Context;\n" +
            "public abstract class Provider {\n" +
            "    private final Context context;\n" +
            "    protected Provider(Context context) {\n" +
            "        this.context = context;\n" +
            "    }\n" +
            "}\n"
        );

        client = JavaFileObjects.forSourceString("pw.thedrhax.mosmetro.httpclient.Client", "" +
            "package pw.thedrhax.mosmetro.httpclient;\n" +
            "public abstract class Client {\n" +
            "}\n"
        );
    }

    @Test public void zeroAnnotatedMethod() {
        assertAbout(javaSources())
            .that(Arrays.asList(context, nonNull, unknown, provider, client))
            .processedWith(new MatchableProcessor())
            .compilesWithoutError();
    }

    @Test public void oneAnnotatedMethod() {
        JavaFileObject expected = JavaFileObjects.forSourceString("pw.thedrhax.mosmetro.authenticator.ProviderChooser", "" +
            "package " + "pw.thedrhax.mosmetro.authenticator" + ";" +
            "import android.content.Context;\n" +
            "import android.support.annotation.NonNull;" +
            "import pw.thedrhax.mosmetro.authenticator.providers.ConcreteProvider;\n" +
            "import pw.thedrhax.mosmetro.authenticator.providers.Unknown;\n" +
            "import pw.thedrhax.mosmetro.httpclient.Client;\n" +
            "public final class ProviderChooser {\n" +
            "@NonNull public static Provider check(@NonNull Context context, @NonNull Client client) {\n" +
            " if (ConcreteProvider.match(client)) {\n" +
            "      return new ConcreteProvider(context);\n" +
            "    }\n" +
            "    return new Unknown(context);\n" +
            "  }\n" +
            "}"
        );

        assertAbout(javaSources())
            .that(Arrays.asList(context, nonNull, concreteProvider, unknown, provider, client))
            .processedWith(new MatchableProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(expected);
    }

    @Test public void twoAnnotatedMethods() {
        JavaFileObject expected = JavaFileObjects.forSourceString("pw.thedrhax.mosmetro.authenticator.ProviderChooser", "" +
            "package " + "pw.thedrhax.mosmetro.authenticator" + ";" +
            "import android.content.Context;\n" +
            "import android.support.annotation.NonNull;" +
            "import pw.thedrhax.mosmetro.authenticator.providers.ConcreteProvider;\n" +
            "import pw.thedrhax.mosmetro.authenticator.providers.ConcreteProvider2;\n" +
            "import pw.thedrhax.mosmetro.authenticator.providers.Unknown;\n" +
            "import pw.thedrhax.mosmetro.httpclient.Client;\n" +
            "public final class ProviderChooser {\n" +
            "@NonNull public static Provider check(@NonNull Context context, @NonNull Client client) {\n" +
            " if (ConcreteProvider.match(client)) {\n" +
            "      return new ConcreteProvider(context);\n" +
            "   } else if (ConcreteProvider2.match(client)) {\n" +
            "      return new ConcreteProvider2(context);\n" +
            "   }\n" +
            " return new Unknown(context);\n" +
            " }\n" +
            "}"
        );

        assertAbout(javaSources())
            .that(Arrays.asList(context, nonNull, concreteProvider, concreteProvider2, unknown, provider, client))
            .processedWith(new MatchableProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(expected);
    }

    @Test public void threeAnnotatedMethods() {
        JavaFileObject expected = JavaFileObjects.forSourceString("pw.thedrhax.mosmetro.authenticator.ProviderChooser", "" +
            "package " + "pw.thedrhax.mosmetro.authenticator" + ";" +
            "import android.content.Context;\n" +
            "import android.support.annotation.NonNull;" +
            "import pw.thedrhax.mosmetro.authenticator.providers.ConcreteProvider;\n" +
            "import pw.thedrhax.mosmetro.authenticator.providers.ConcreteProvider2;\n" +
            "import pw.thedrhax.mosmetro.authenticator.providers.ConcreteProvider3;\n" +
            "import pw.thedrhax.mosmetro.authenticator.providers.Unknown;\n" +
            "import pw.thedrhax.mosmetro.httpclient.Client;\n" +
            "public final class ProviderChooser {\n" +
            "@NonNull public static Provider check(@NonNull Context context, @NonNull Client client) {\n" +
            " if (ConcreteProvider.match(client)) {\n" +
            "      return new ConcreteProvider(context);\n" +
            "   } else if (ConcreteProvider2.match(client)) {\n" +
            "      return new ConcreteProvider2(context);\n" +
            "   } else if (ConcreteProvider3.match(client)) {\n" +
            "      return new ConcreteProvider3(context);\n" +
            "   }\n" +
            " return new Unknown(context);\n" +
            " }\n" +
            "}"
        );

        assertAbout(javaSources())
            .that(Arrays.asList(context, nonNull, concreteProvider, concreteProvider2, concreteProvider3, unknown, provider, client))
            .processedWith(new MatchableProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(expected);
    }

    @Test public void fourAnnotatedMethods() {
        JavaFileObject expected = JavaFileObjects.forSourceString("pw.thedrhax.mosmetro.authenticator.ProviderChooser", "" +
            "package " + "pw.thedrhax.mosmetro.authenticator" + ";" +
            "import android.content.Context;\n" +
            "import android.support.annotation.NonNull;" +
            "import pw.thedrhax.mosmetro.authenticator.providers.ConcreteProvider;\n" +
            "import pw.thedrhax.mosmetro.authenticator.providers.ConcreteProvider2;\n" +
            "import pw.thedrhax.mosmetro.authenticator.providers.ConcreteProvider3;\n" +
            "import pw.thedrhax.mosmetro.authenticator.providers.ConcreteProvider4;\n" +
            "import pw.thedrhax.mosmetro.authenticator.providers.Unknown;\n" +
            "import pw.thedrhax.mosmetro.httpclient.Client;\n" +
            "public final class ProviderChooser {\n" +
            "@NonNull public static Provider check(@NonNull Context context, @NonNull Client client) {\n" +
            " if (ConcreteProvider.match(client)) {\n" +
            "      return new ConcreteProvider(context);\n" +
            "   } else if (ConcreteProvider2.match(client)) {\n" +
            "      return new ConcreteProvider2(context);\n" +
            "   } else if (ConcreteProvider3.match(client)) {\n" +
            "      return new ConcreteProvider3(context);\n" +
            "   } else if (ConcreteProvider4.match(client)) {\n" +
            "      return new ConcreteProvider4(context);\n" +
            "   }\n" +
            " return new Unknown(context);\n" +
            " }\n" +
            "}"
        );

        assertAbout(javaSources())
            .that(
                Arrays.asList(context, nonNull,
                    concreteProvider, concreteProvider2, concreteProvider3, concreteProvider4,
                    unknown, provider, client)
            )
            .processedWith(new MatchableProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(expected);
    }

    @Test public void annotatedMethodIsNotStatic() {
        JavaFileObject concreteProviderNotStatic = JavaFileObjects.forSourceString(
            "pw.thedrhax.mosmetro.authenticator.providers.ConcreteProviderNotStatic",
            "package pw.thedrhax.mosmetro.authenticator.providers;\n" +
                "import android.content.Context;\n" +
                "import java.util.Random;\n" +
                "import pw.thedrhax.mosmetro.Matchable;\n" +
                "import pw.thedrhax.mosmetro.authenticator.Provider;\n" +
                "import pw.thedrhax.mosmetro.httpclient.Client;\n" +
                "public class ConcreteProviderNotStatic extends Provider {\n" +
                "    private static final Random random = new Random();\n" +
                "    public ConcreteProviderNotStatic(Context context) {\n" +
                "        super(context);\n" +
                "    }\n" +
                "    @Matchable public boolean match(Client client) {\n" +
                "        return random.nextBoolean();\n" +
                "    }\n" +
                "}\n"
        );
        assertAbout(javaSources())
            .that(Arrays.asList(context, nonNull, concreteProviderNotStatic, unknown, provider, client))
            .processedWith(new MatchableProcessor())
            .failsToCompile()
            .withErrorCount(1)
            .withErrorContaining("Methods annotated with " + ANNOTATION + " must be public and static.");
    }

    @Test public void annotatedMethodIsNotPublic() {
        JavaFileObject concreteProviderNotPublic = JavaFileObjects.forSourceString(
            "pw.thedrhax.mosmetro.authenticator.providers.ConcreteProviderNotPublic",
            "package pw.thedrhax.mosmetro.authenticator.providers;\n" +
                "import android.content.Context;\n" +
                "import java.util.Random;\n" +
                "import pw.thedrhax.mosmetro.Matchable;\n" +
                "import pw.thedrhax.mosmetro.authenticator.Provider;\n" +
                "import pw.thedrhax.mosmetro.httpclient.Client;\n" +
                "public class ConcreteProviderNotPublic extends Provider {\n" +
                "    private static final Random random = new Random();\n" +
                "    public ConcreteProviderNotPublic(Context context) {\n" +
                "        super(context);\n" +
                "    }\n" +
                "    @Matchable static boolean match(Client client) {\n" +
                "        return random.nextBoolean();\n" +
                "    }\n" +
                "}\n"
        );
        assertAbout(javaSources())
            .that(Arrays.asList(context, nonNull, concreteProviderNotPublic, unknown, provider, client))
            .processedWith(new MatchableProcessor())
            .failsToCompile()
            .withErrorCount(1)
            .withErrorContaining("Methods annotated with " + ANNOTATION + " must be public and static.");
    }

    @Test public void annotatedMethodHasNoModifier() {
        JavaFileObject concreteProviderNoModifier = JavaFileObjects.forSourceString(
            "pw.thedrhax.mosmetro.authenticator.providers.ConcreteProviderNoModifier",
            "package pw.thedrhax.mosmetro.authenticator.providers;\n" +
                "import android.content.Context;\n" +
                "import java.util.Random;\n" +
                "import pw.thedrhax.mosmetro.Matchable;\n" +
                "import pw.thedrhax.mosmetro.authenticator.Provider;\n" +
                "import pw.thedrhax.mosmetro.httpclient.Client;\n" +
                "public class ConcreteProviderNoModifier extends Provider {\n" +
                "    private static final Random random = new Random();\n" +
                "    public ConcreteProviderNoModifier(Context context) {\n" +
                "        super(context);\n" +
                "    }\n" +
                "    @Matchable boolean match(Client client) {\n" +
                "        return random.nextBoolean();\n" +
                "    }\n" +
                "}\n"
        );
        assertAbout(javaSources())
            .that(Arrays.asList(context, nonNull, concreteProviderNoModifier, unknown, provider, client))
            .processedWith(new MatchableProcessor())
            .failsToCompile()
            .withErrorCount(1)
            .withErrorContaining("Methods annotated with " + ANNOTATION + " must be public and static.");
    }

    @Test public void annotatedMethodHasNoParameters() {
        JavaFileObject concreteProviderNoParams = JavaFileObjects.forSourceString(
            "pw.thedrhax.mosmetro.authenticator.providers.ConcreteProviderNoParams",
            "package pw.thedrhax.mosmetro.authenticator.providers;\n" +
                "import android.content.Context;\n" +
                "import java.util.Random;\n" +
                "import pw.thedrhax.mosmetro.Matchable;\n" +
                "import pw.thedrhax.mosmetro.authenticator.Provider;\n" +
                "import pw.thedrhax.mosmetro.httpclient.Client;\n" +
                "public class ConcreteProviderNoParams extends Provider {\n" +
                "    private static final Random random = new Random();\n" +
                "    public ConcreteProviderNoParams(Context context) {\n" +
                "        super(context);\n" +
                "    }\n" +
                "    @Matchable public static boolean match() {\n" +
                "        return random.nextBoolean();\n" +
                "    }\n" +
                "}\n"
        );
        assertAbout(javaSources())
            .that(Arrays.asList(context, nonNull, concreteProviderNoParams, unknown, provider, client))
            .processedWith(new MatchableProcessor())
            .failsToCompile()
            .withErrorCount(1)
            .withErrorContaining(
                String.format(
                    "Methods annotated with %1$s must have '%2$s' parameter 'client' because it is required by design.",
                    ANNOTATION,
                    CLIENT_CLASS_PARAMETER
                ));
    }

    @Test public void annotatedMethodHasWrongParameter() {
        JavaFileObject concreteProviderWrongParam = JavaFileObjects.forSourceString(
            "pw.thedrhax.mosmetro.authenticator.providers.ConcreteProviderWrongParam",
            "package pw.thedrhax.mosmetro.authenticator.providers;\n" +
                "import android.content.Context;\n" +
                "import java.util.Random;\n" +
                "import pw.thedrhax.mosmetro.Matchable;\n" +
                "import pw.thedrhax.mosmetro.authenticator.Provider;\n" +
                "import pw.thedrhax.mosmetro.httpclient.Client;\n" +
                "public class ConcreteProviderWrongParam extends Provider {\n" +
                "    private static final Random random = new Random();\n" +
                "    public ConcreteProviderWrongParam(Context context) {\n" +
                "        super(context);\n" +
                "    }\n" +
                "    @Matchable public static boolean match(String value) {\n" +
                "        return random.nextBoolean();\n" +
                "    }\n" +
                "}\n"
        );
        assertAbout(javaSources())
            .that(Arrays.asList(context, nonNull, concreteProviderWrongParam, unknown, provider, client))
            .processedWith(new MatchableProcessor())
            .failsToCompile()
            .withErrorCount(1)
            .withErrorContaining(
                String.format(
                    "Methods annotated with %1$s must have '%2$s' parameter 'client' because it is required by design.",
                    ANNOTATION,
                    CLIENT_CLASS_PARAMETER
                ));
    }

    @Test public void annotatedMethodHasTooManyParameters() {
        JavaFileObject concreteProviderTooManyParams = JavaFileObjects.forSourceString(
            "pw.thedrhax.mosmetro.authenticator.providers.ConcreteProviderTooManyParams",
            "package pw.thedrhax.mosmetro.authenticator.providers;\n" +
                "import android.content.Context;\n" +
                "import java.util.Random;\n" +
                "import pw.thedrhax.mosmetro.Matchable;\n" +
                "import pw.thedrhax.mosmetro.authenticator.Provider;\n" +
                "import pw.thedrhax.mosmetro.httpclient.Client;\n" +
                "public class ConcreteProviderTooManyParams extends Provider {\n" +
                "    private static final Random random = new Random();\n" +
                "    public ConcreteProviderTooManyParams(Context context) {\n" +
                "        super(context);\n" +
                "    }\n" +
                "    @Matchable public static boolean match(Client client, String value) {\n" +
                "        return random.nextBoolean();\n" +
                "    }\n" +
                "}\n"
        );
        assertAbout(javaSources())
            .that(Arrays.asList(context, nonNull, concreteProviderTooManyParams, unknown, provider, client))
            .processedWith(new MatchableProcessor())
            .failsToCompile()
            .withErrorCount(1)
            .withErrorContaining(
                String.format(
                    "Methods annotated with %1$s must have '%2$s' parameter 'client' because it is required by design.",
                    ANNOTATION,
                    CLIENT_CLASS_PARAMETER
                ));
    }

    @Test public void annotatedMethodHasReturnTypeNotBoolean() {
        JavaFileObject concreteProviderReturnTypeNotBoolean = JavaFileObjects.forSourceString(
            "pw.thedrhax.mosmetro.authenticator.providers.ConcreteProviderReturnTypeNotBoolean",
            "package pw.thedrhax.mosmetro.authenticator.providers;\n" +
                "import android.content.Context;\n" +
                "import java.util.Random;\n" +
                "import pw.thedrhax.mosmetro.Matchable;\n" +
                "import pw.thedrhax.mosmetro.authenticator.Provider;\n" +
                "import pw.thedrhax.mosmetro.httpclient.Client;\n" +
                "public class ConcreteProviderReturnTypeNotBoolean extends Provider {\n" +
                "    private static final Random random = new Random();\n" +
                "    public ConcreteProviderReturnTypeNotBoolean(Context context) {\n" +
                "        super(context);\n" +
                "    }\n" +
                "    @Matchable public static int match(Client client) {\n" +
                "        return random.nextInt();\n" +
                "    }\n" +
                "}\n"
        );
        assertAbout(javaSources())
            .that(Arrays.asList(context, nonNull, concreteProviderReturnTypeNotBoolean, unknown, provider, client))
            .processedWith(new MatchableProcessor())
            .failsToCompile()
            .withErrorCount(1)
            .withErrorContaining(
                String.format(
                    "Methods annotated with %1$s must have return type of unboxed primitive boolean.",
                    ANNOTATION
                ));
    }

    @Test public void annotatedMethodHasReturnTypeOfBoxedBoolean() {
        JavaFileObject concreteProviderReturnTypeIsBoxedBoolean = JavaFileObjects.forSourceString(
            "pw.thedrhax.mosmetro.authenticator.providers.ConcreteProviderReturnTypeIsBoxedBoolean",
            "package pw.thedrhax.mosmetro.authenticator.providers;\n" +
                "import android.content.Context;\n" +
                "import java.util.Random;\n" +
                "import pw.thedrhax.mosmetro.Matchable;\n" +
                "import pw.thedrhax.mosmetro.authenticator.Provider;\n" +
                "import pw.thedrhax.mosmetro.httpclient.Client;\n" +
                "public class ConcreteProviderReturnTypeIsBoxedBoolean extends Provider {\n" +
                "    private static final Random random = new Random();\n" +
                "    public ConcreteProviderReturnTypeIsBoxedBoolean(Context context) {\n" +
                "        super(context);\n" +
                "    }\n" +
                "    @Matchable public static Boolean match(Client client) {\n" +
                "        return random.nextBoolean();\n" +
                "    }\n" +
                "}\n"
        );

        assertAbout(javaSources())
            .that(Arrays.asList(context, nonNull, concreteProviderReturnTypeIsBoxedBoolean, unknown, provider, client))
            .processedWith(new MatchableProcessor())
            .failsToCompile()
            .withErrorCount(1)
            .withErrorContaining(
                String.format(
                    "Methods annotated with %1$s must have return type of unboxed primitive boolean.",
                    ANNOTATION
                ));
    }

    @Test public void annotatedMethodIsNotDescendant() {
        JavaFileObject concreteProviderIsNotDescendantOfProvider = JavaFileObjects.forSourceString(
            "pw.thedrhax.mosmetro.authenticator.providers.ConcreteProvider", "" +
                "package pw.thedrhax.mosmetro.authenticator.providers;\n" +
                "import android.content.Context;\n" +
                "import java.util.Random;\n" +
                "import pw.thedrhax.mosmetro.Matchable;\n" +
                "import pw.thedrhax.mosmetro.authenticator.Provider;\n" +
                "import pw.thedrhax.mosmetro.httpclient.Client;\n" +
                "public class ConcreteProvider {\n" +
                "    private static final Random random = new Random();\n" +
                "    public ConcreteProvider(Context context) {\n" +
                "        super(context);\n" +
                "    }\n" +
                "    @Matchable public static boolean match(Client client) {\n" +
                "        return random.nextBoolean();\n" +
                "    }\n" +
                "}\n"
        );
        assertAbout(javaSources())
            .that(Arrays.asList(context, nonNull, concreteProviderIsNotDescendantOfProvider, unknown, provider, client))
            .processedWith(new MatchableProcessor())
            .failsToCompile()
            .withErrorCount(1)
            .withErrorContaining(
                String.format(
                    "Methods annotated with %1$s must extend %2$s.",
                    ANNOTATION,
                    PROVIDER_CLASS_PARAMETER
                ));
    }

    @Test public void twoAnnotatedMethodsInOneClass() {
        JavaFileObject concreteProviderIsNotDescendantOfProvider = JavaFileObjects.forSourceString(
            "pw.thedrhax.mosmetro.authenticator.providers.ConcreteProvider", "" +
                "package pw.thedrhax.mosmetro.authenticator.providers;\n" +
                "import android.content.Context;\n" +
                "import java.util.Random;\n" +
                "import pw.thedrhax.mosmetro.Matchable;\n" +
                "import pw.thedrhax.mosmetro.authenticator.Provider;\n" +
                "import pw.thedrhax.mosmetro.httpclient.Client;\n" +
                "public class ConcreteProvider extends Provider {\n" +
                "    private static final Random random = new Random();\n" +
                "    public ConcreteProvider(Context context) {\n" +
                "        super(context);\n" +
                "    }\n" +
                "    @Matchable public static boolean match(Client client) {\n" +
                "        return random.nextBoolean();\n" +
                "    }\n" +
                "    @Matchable public static boolean match2(Client client) {\n" +
                "        return random.nextBoolean();\n" +
                "    }\n" +
                "}\n"
        );
        assertAbout(javaSources())
            .that(Arrays.asList(context, nonNull, concreteProviderIsNotDescendantOfProvider, unknown, provider, client))
            .processedWith(new MatchableProcessor())
            .failsToCompile()
            .withErrorCount(1)
            .withErrorContaining("Only one " + ANNOTATION + " method can be declared in the class.");
    }

    @Test public void multipleErrorsClass() {
        JavaFileObject concreteProviderIsNotDescendantOfProvider = JavaFileObjects.forSourceString(
            "pw.thedrhax.mosmetro.authenticator.providers.ConcreteProvider", "" +
                "package pw.thedrhax.mosmetro.authenticator.providers;\n" +
                "import android.content.Context;\n" +
                "import java.util.Random;\n" +
                "import pw.thedrhax.mosmetro.Matchable;\n" +
                "import pw.thedrhax.mosmetro.authenticator.Provider;\n" +
                "import pw.thedrhax.mosmetro.httpclient.Client;\n" +
                "public class ConcreteProvider extends Provider {\n" +
                "    private static final Random random = new Random();\n" +
                "    public ConcreteProvider(Context context) {\n" +
                "        super(context);\n" +
                "    }\n" +
                "    @Matchable public static boolean match(Client client) {\n" +
                "        return random.nextBoolean();\n" +
                "    }\n" +
                "    @Matchable public boolean match2(Client client) {\n" +
                "        return random.nextBoolean();\n" +
                "    }\n" +
                "}\n"
        );
        assertAbout(javaSources())
            .that(Arrays.asList(context, nonNull, concreteProviderIsNotDescendantOfProvider, unknown, provider, client))
            .processedWith(new MatchableProcessor())
            .failsToCompile()
            .withErrorCount(2)
            .withErrorContaining(
                "Methods annotated with @Matchable must be public and static."
            );
    }
}
