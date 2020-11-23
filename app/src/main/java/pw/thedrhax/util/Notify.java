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
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;

public class Notify extends NotificationCompat.Builder {
    private Context context;
    private NotificationManager nm;
    private SharedPreferences settings;

    private int id = 0;
    private boolean enabled = true;
    private boolean locked = false;
    private boolean big_text = true;

    public Notify(Context context) {
        super(context);
        this.context = context;
        this.nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);

        priority(Util.getIntPreference(context, "pref_notify_priority", 0));
    }

    public Notify title(String title) {
        setContentTitle(title); return this;
    }

    public Notify text(String text) {
        setContentText(text);
        if (big_text) style(new NotificationCompat.BigTextStyle().bigText(text));
        return this;
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

    public Notify priority(int priority) {
        if (priority < -2) priority = -2;
        if (priority > 2) priority = 2;
        setPriority(priority); return this;
    }

    public Notify icon(int colored, int white) {
        boolean pref_colored = (Build.VERSION.SDK_INT <= 20) ^
                settings.getBoolean("pref_notify_alternative", false);

        setSmallIcon(pref_colored ? colored : white); return this;
    }

    public Notify style(NotificationCompat.Style style) {
        big_text = style instanceof NotificationCompat.BigTextStyle;
        setStyle(style); return this;
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

    public Notify addAction(CharSequence title, PendingIntent intent) {
        super.addAction(0, title, intent); return this;
    }

    public Notify enabled(boolean enabled) {
        this.enabled = enabled; return this;
    }

    public Notify locked(boolean locked) {
        this.locked = locked; return this;
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
