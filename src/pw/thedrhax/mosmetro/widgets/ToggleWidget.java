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

package pw.thedrhax.mosmetro.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.services.ConnectionService;

public class ToggleWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Construct intent
        Intent intent = new Intent(context, ToggleWidget.class);
        intent.putExtra("toggle", "pref_autoconnect");
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        // Construct widget
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.toggle_widget);
        views.setOnClickPendingIntent(R.id.imageButton, pIntent);

        boolean pref_autoconnect = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean("pref_autoconnect", true);
        views.setImageViewResource(R.id.imageButton, pref_autoconnect ?
                        R.drawable.ic_widget_toggle_on :
                        R.drawable.ic_widget_toggle_off);

        for (int appWidgetId : appWidgetIds) { // For all widgets
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // Get preferences and bundle
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        Bundle bundle = intent.getExtras();

        // Toggle pref_autoconnect if set
        if (bundle != null && "pref_autoconnect".equals(bundle.getString("toggle"))) {
            boolean pref_autoconnect = !settings.getBoolean("pref_autoconnect", true);

            settings.edit()
                    .putBoolean("pref_autoconnect", pref_autoconnect)
                    .apply();

            Intent service = new Intent(context, ConnectionService.class);
            if (!pref_autoconnect)
                service.setAction("STOP");
            context.startService(service);
        }

        // Update widget
        AppWidgetManager wm = AppWidgetManager.getInstance(context);
        ComponentName cm = new ComponentName(context, ToggleWidget.class);
        onUpdate(context, wm, wm.getAppWidgetIds(cm));
    }
}
