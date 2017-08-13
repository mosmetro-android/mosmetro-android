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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.SystemClock;

import pw.thedrhax.util.Listener;
import pw.thedrhax.util.Util;

public class CaptchaRecognitionProxy {
    private Context context = null;

    public CaptchaRecognitionProxy(Context context) {
        this.context = context;
    }

    public String recognize(final Bitmap bitmap) throws Exception {
        final String[] reply = new String[]{null};

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                reply[0] = intent.getStringExtra("code");
            }
        };
        context.registerReceiver(receiver, new IntentFilter("pw.thedrhax.captcharecognition.RESULT"));

        context.startService(
                new Intent("pw.thedrhax.captcharecognition.RECOGNIZE")
                        .setPackage("pw.thedrhax.captcharecognition")
                        .putExtra("bitmap_base64", Util.bitmapToBase64(bitmap))
        );

        // Wait for answer
        int timeout = 20; // 2 seconds
        while (!isStopped() && reply[0] == null && timeout-- != 0) {
            SystemClock.sleep(100);
        }

        context.unregisterReceiver(receiver);

        return reply[0];
    }

    /**
     * Listener used to stop Provider immediately after
     * variable is changed by another thread
     */
    private final Listener<Boolean> running = new Listener<>(true);

    /**
     * Subscribe to another Listener to implement cascade notifications
     */
    public CaptchaRecognitionProxy setRunningListener(Listener<Boolean> master) {
        running.subscribe(master); return this;
    }

    /**
     * Method used to check if Provider must finish as soon as possible.
     * @return true is Provider must stop, otherwise false.
     */
    private boolean isStopped() {
        return !running.get();
    }
}
