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
                        R.drawable.ic_notification_success_colored :
                        R.drawable.ic_notification_error_colored);

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
        if ((bundle != null) && ("pref_autoconnect".equals(bundle.getString("toggle"))))
            settings.edit()
                    .putBoolean("pref_autoconnect", !settings.getBoolean("pref_autoconnect", true))
                    .apply();

        // Update widget
        AppWidgetManager wm = AppWidgetManager.getInstance(context);
        ComponentName cm = new ComponentName(context, ToggleWidget.class);
        onUpdate(context, wm, wm.getAppWidgetIds(cm));
    }
}
