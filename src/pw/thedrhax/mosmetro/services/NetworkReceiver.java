package pw.thedrhax.mosmetro.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class NetworkReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        Intent service = new Intent(context, ConnectionService.class);
        service.setAction(intent.getAction());
        service.putExtras(intent);

        if (settings.getBoolean("pref_autoconnect", true))
            context.startService(service);
    }
}
