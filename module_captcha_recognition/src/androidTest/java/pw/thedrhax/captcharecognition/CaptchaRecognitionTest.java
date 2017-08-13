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

package pw.thedrhax.captcharecognition;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.test.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Test;

/**
 * A collection of the CaptchaRecognition class tests
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 */
public class CaptchaRecognitionTest {
    private static final String ASSETS_PATH = "example_captcha";

    private Context context = InstrumentationRegistry.getContext();

    @Test
    public void recognize() throws Exception {
        CaptchaRecognition cr = new CaptchaRecognition(context);

        for (String filename : context.getAssets().list(ASSETS_PATH)) {
            Bitmap bitmap = BitmapFactory.decodeStream(
                    context.getAssets().open(ASSETS_PATH + "/" + filename)
            );
            String code = filename.split(".png")[0];

            Assert.assertEquals(code, code, cr.recognize(bitmap));
        }

        cr.close();
    }
}