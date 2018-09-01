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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A collection of the Util class tests
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 */
public class UtilTest {
    private Context context = InstrumentationRegistry.getContext();

    @Test
    public void getIntPreference() throws Exception {
        assertEquals(123, Util.getIntPreference(context, "none", 123));
    }

    @Test
    public void wellDefinedClass() throws Exception {
        UtilityClasses.assertUtilityClassWellDefined(Util.class);
    }
}