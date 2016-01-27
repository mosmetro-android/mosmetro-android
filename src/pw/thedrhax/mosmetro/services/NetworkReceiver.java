package pw.thedrhax.mosmetro.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import pw.thedrhax.util.Notification;

public class NetworkReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        if (!settings.getBoolean("pref_autoconnect", true)) return;

        SharedPreferences.Editor settings_editor = settings.edit();

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if ("\"MosMetro_Free\"".equals(wifiInfo.getSSID())) {
            if (!settings.getBoolean("locked", false)) {
                settings_editor.putBoolean("locked", true);
                settings_editor.apply();
                context.startService(new Intent(context, ConnectionService.class));
            }
        } else {
            settings_editor.putBoolean("locked", false);
            settings_editor.apply();
            new Notification(context).hide(); // hide remaining notification
        }
    }
}
