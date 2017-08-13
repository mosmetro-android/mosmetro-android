/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 * Copyright © 2015 Dmitry Karikh <the.dr.hax@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pw.thedrhax.util;

import org.junit.Assert;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * A collection of Utility classes tests.
 * @author Savelii Zagurskii <saveliyzagurskiy@gmail.com>
 */
public class UtilityClasses {
    /**
     * Verifies that a utility class is well defined.
     * @param clazz utility class to verify.
     * @see <a href="http://stackoverflow.com/a/10872497/4271064">Stack Overflow</a>
     */
    static void assertUtilityClassWellDefined(Class<?> clazz) throws Exception {
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
}
