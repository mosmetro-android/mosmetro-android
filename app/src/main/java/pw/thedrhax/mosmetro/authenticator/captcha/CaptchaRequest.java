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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;

import java.util.HashMap;
import java.util.Map;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.CaptchaDialog;
import pw.thedrhax.mosmetro.services.ConnectionService;
import pw.thedrhax.util.Listener;
import pw.thedrhax.util.Notify;
import pw.thedrhax.util.Util;

public class CaptchaRequest {
    private static final int STATUS_NONE = 0;
    public static final int STATUS_CLOSED = 1;
    public static final int STATUS_ENTERED = 2;

    private final Listener<Boolean> running = new Listener<>(true);
    private final Context context;
    private final boolean from_debug;

    public CaptchaRequest(Context context) {
        this.context = context;

        if (context instanceof ConnectionService) {
            from_debug = ((ConnectionService)context).isFromDebug();
        } else {
            from_debug = false;
        }
    }

    public CaptchaRequest setRunningListener(Listener<Boolean> master) {
        running.subscribe(master); return this;
    }

    public Map<String,String> getResult(Bitmap image, String code) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        final String image_base64 = Util.bitmapToBase64(image);
        final int[] status = new int[]{0};
        final Map<String,String> result = new HashMap<>();
        result.put("captcha_image", image_base64);

        final Intent captcha_activity = new Intent(context, CaptchaDialog.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("image", image_base64)
                .putExtra("code", code);

        if (!from_debug)
            captcha_activity
                    .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    .putExtra("finish_on_pause", true);

        final Notify captcha_notify = new Notify(context).id(2)
                .title(context.getString(R.string.notification_captcha))
                .text(context.getString(R.string.notification_captcha_summary))
                .icon(R.drawable.ic_notification_register)
                .onClick(PendingIntent.getActivity(
                        context, 254, captcha_activity, PendingIntent.FLAG_UPDATE_CURRENT
                ))
                .onDelete(PendingIntent.getService(
                        context, 253,
                        new Intent(context, ConnectionService.class).setAction("STOP"),
                        PendingIntent.FLAG_UPDATE_CURRENT
                ));

        // Reply directly from notifications
        if (Build.VERSION.SDK_INT >= 24 && settings.getBoolean("pref_notify_reply", false)) {
            final RemoteInput input = new RemoteInput.Builder("key_captcha_reply")
                    .setLabel(context.getString(R.string.reply))
                    .build();

            NotificationCompat.Action action = new NotificationCompat.Action.Builder(
                    R.drawable.ic_notification_register,
                    context.getString(R.string.reply),
                    PendingIntent.getBroadcast(
                            context, 252,
                            new Intent("pw.thedrhax.mosmetro.event.CAPTCHA_RESULT")
                                    .putExtra("status", CaptchaRequest.STATUS_ENTERED),
                            PendingIntent.FLAG_UPDATE_CURRENT
                    )
            ).addRemoteInput(input).build();

            captcha_notify
                    .text("")
                    .onClick(null)
                    .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(image))
                    .addAction(action);
        }

        boolean auto_activity = from_debug || settings.getBoolean("pref_captcha_dialog", true);

        // Asking user to enter the code
        if (auto_activity)
            context.startActivity(captcha_activity);
        else
            captcha_notify.show();

        // Register result receiver
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
                if (remoteInput != null) { // reply from notification
                    result.put("captcha_code",
                            Util.convertCyrillicSymbols(remoteInput.getString("key_captcha_reply"))
                    );
                } else { // reply from dialog
                    result.put("captcha_code", intent.getStringExtra("value"));
                }
                status[0] = intent.getIntExtra("status", STATUS_NONE);
            }
        };
        context.getApplicationContext().registerReceiver(
                receiver, new IntentFilter("pw.thedrhax.mosmetro.event.CAPTCHA_RESULT")
        );

        // Wait for answer
        while (status[0] != STATUS_ENTERED && running.get()) {
            if (status[0] == STATUS_CLOSED) {
                if (!from_debug)
                    captcha_notify.show();
                else
                    break;
            }
            SystemClock.sleep(100);
        }

        // Unregister receiver, close the Activity and remove the Notification
        context.getApplicationContext().unregisterReceiver(receiver);
        if (!running.get() && auto_activity)
            context.startActivity(
                    new Intent(context, CaptchaDialog.class).setAction("STOP")
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            );
        captcha_notify.hide();

        return result;
    }
}
