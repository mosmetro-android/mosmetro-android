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

package pw.thedrhax.mosmetro.authenticator.captcha;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class CaptchaRecognition extends TensorFlowInferenceInterface {
    private static final String MODEL_FILE = "file:///android_asset/frozen_graph_ler=0.02.pb";

    private static final int WIDTH = 152;
    private static final int HEIGHT = 48;
    private static final int DEPTH = 3;

    private static final int CHARS = 4;
    private static final String CODEC = "-0123456789abcdef";

    private final float[] input;
    private final int[] output;

    public CaptchaRecognition(Context context) {
        super(context.getAssets(), MODEL_FILE);
        graphOperation("Placeholder");

        input = new float[WIDTH * HEIGHT * DEPTH];
        output = new int[CHARS];
    }

    public String recognize(final Bitmap bitmap) {
        final Bitmap scaled_bitmap = Bitmap.createScaledBitmap(bitmap, WIDTH, HEIGHT, true);

        // Convert Bitmap to float[]
        int pixel, pos;
        for (int y = 0; y < scaled_bitmap.getHeight(); y++) {
            for (int x = 0; x < scaled_bitmap.getWidth(); x++) {
                pixel = scaled_bitmap.getPixel(x, y);
                pos = (scaled_bitmap.getHeight() * x + y) * 3;

                input[pos + 0] = Color.blue(pixel) / (float) 255 - (float) 0.5;
                input[pos + 1] = Color.green(pixel) / (float) 255 - (float) 0.5;
                input[pos + 2] = Color.red(pixel) / (float) 255 - (float) 0.5;
            }
        }

        feed("Placeholder", input, 1, WIDTH, HEIGHT, DEPTH);
        run(new String[]{"decoded_indexes"});
        fetch("decoded_indexes", output);

        // Convert output to String
        StringBuilder result = new StringBuilder();
        for (int i : output) {
            result.append(CODEC.charAt(i));
        }

        return result.toString();
    }
}
