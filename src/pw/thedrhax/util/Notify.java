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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.support.v7.app.NotificationCompat;

public class Notify extends NotificationCompat.Builder {
    private Context context;
    private NotificationManager nm;

    private int id = 0;
    private boolean enabled = true;
    private boolean locked = false;

    public Notify(Context context) {
        super(context);
        this.context = context;
        this.nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public Notify title(String title) {
        setContentTitle(title); return this;
    }

    public Notify text(String text) {
        setContentText(text); return this;
    }

    public Notify onClick(PendingIntent intent) {
        setContentIntent(intent); return this;
    }

    public Notify onDelete(PendingIntent intent) {
        setDeleteIntent(intent); return this;
    }

    public Notify id(int id) {
        this.id = id; return this;
    }

    public Notify icon(int icon) {
        setSmallIcon(icon); return this;
    }

    public Notify progress(int progress, boolean indeterminate) {
        setProgress(100, progress, indeterminate); return this;
    }

    public Notify progress(int progress) {
        return progress(progress, false);
    }

    public Notify hideProgress() {
        setProgress(0, 0, false); return this;
    }

    public Notify addAction(int icon, CharSequence title, PendingIntent intent) {
        super.addAction(icon, title, intent); return this;
    }

    public Notify enabled(boolean enabled) {
        this.enabled = enabled; return this;
    }

    public Notify locked(boolean locked) {
        this.locked = locked; return this;
    }

    public boolean isLocked() {
        return locked;
    }

    public Notify cancelOnClick(boolean cancel) {
        setAutoCancel(cancel); return this;
    }

    public Notify show() {
        if (!enabled) return this;

        Notification notification = build();

        if (locked && context instanceof Service) {
            ((Service) context).startForeground(id, notification);
            return this;
        }

        if (locked) {
            notification.flags |= Notification.FLAG_NO_CLEAR;
        }

        nm.notify(id, build()); return this;
    }

    public Notify hide() {
        if (locked && context instanceof Service) {
            ((Service) context).stopForeground(true);
        } else {
            nm.cancel(id);
        }
        return this;
    }
}
