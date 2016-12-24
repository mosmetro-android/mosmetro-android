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

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Build;

public class Notification {
    private Context context;
    private android.app.Notification.Builder builder;
    private NotificationManager nm;

    private int id = 0;
    private boolean cancellable;
    private boolean enabled = true;

    public Notification (Context context) {
        this.context = context;
        this.builder = new android.app.Notification.Builder(context);
        this.nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        setCancellable(true);
    }

    public Notification setTitle (String title) {
        builder = builder.setContentTitle(title);
        return this;
    }

    public Notification setText (String text) {
        builder = builder.setContentText(text);

        if (Build.VERSION.SDK_INT >= 16) // show expandable text on newer devices
            builder = builder.setStyle(new android.app.Notification.BigTextStyle().bigText(text));
        return this;
    }

    public Notification setIntent (Intent intent) {
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder = builder.setContentIntent(pIntent);
        return this;
    }

    public Notification setDeleteIntent (PendingIntent intent) {
        builder = builder.setDeleteIntent(intent);
        return this;
    }

    public Notification setId (int id) {
        this.id = id;
        return this;
    }

    public Notification setIcon (int icon) {
        builder = builder
                .setSmallIcon(icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), icon));

        return this;
    }

    public Notification setCancellable (boolean cancellable) {
        this.cancellable = cancellable;
        builder = builder.setAutoCancel(cancellable); // delete notification on press if true
        return this;
    }

    public Notification setContinuous () {
        if (Build.VERSION.SDK_INT >= 14)
            builder = builder.setProgress(0, 0, true);
        return this;
    }

    public Notification setProgress (int progress) {
        if (Build.VERSION.SDK_INT >= 14)
            builder = builder.setProgress(100, progress, false);
        return this;
    }

    public Notification setEnabled (boolean enabled) {
        this.enabled = enabled; return this;
    }

    public void show() {
        if (!enabled) return;

        android.app.Notification notification;
        if (Build.VERSION.SDK_INT >= 16) {
            notification = builder.build();
        } else { // support older devices
            notification = builder.getNotification();
        }
        if (!cancellable) notification.flags |= android.app.Notification.FLAG_NO_CLEAR;
        nm.notify(id, notification);
    }

    public void hide() {
        nm.cancel(id);
    }
}
