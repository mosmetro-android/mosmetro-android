package pw.thedrhax.mosmetro;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import pw.thedrhax.util.AndroidHacks;
import pw.thedrhax.util.Util;
import pw.thedrhax.util.Version;

/**
 * A collection of Utility classes tests.
 * @author Savelii Zagurskii <saveliyzagurskiy@gmail.com>
 */
public class UtilityClassesTest {
    /**
     * Verifies that a utility class is well defined.
     * @param clazz utility class to verify.
     * @see <a href="http://stackoverflow.com/a/10872497/4271064">Stack Overflow</a>
     */
    private static void assertUtilityClassWellDefined(Class<?> clazz) throws Exception {
        Assert.assertTrue("Class must be final", Modifier.isFinal(clazz.getModifiers()));
        Assert.assertEquals(
                "Only one constructor allowed", 1,
                clazz.getDeclaredConstructors().length
        );

        Constructor<?> constructor = clazz.getDeclaredConstructor();
        if (constructor.isAccessible() || !Modifier.isPrivate(constructor.getModifiers())) {
            Assert.fail("Constructor is not private");
        }
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);

        for (Method method : clazz.getMethods())
            if (!Modifier.isStatic(method.getModifiers()))
                if (method.getDeclaringClass().equals(clazz))
                    Assert.fail("There exists a non-static method: " + method);
    }

    @Test
    public void checkUtilityClass_AndroidHacks() throws Exception {
        assertUtilityClassWellDefined(AndroidHacks.class);
    }

    @Test
    public void checkUtilityClass_Util() throws Exception {
        assertUtilityClassWellDefined(Util.class);
    }

    @Test
    public void checkUtilityClass_Version() throws Exception {
        assertUtilityClassWellDefined(Version.class);
    }
}
