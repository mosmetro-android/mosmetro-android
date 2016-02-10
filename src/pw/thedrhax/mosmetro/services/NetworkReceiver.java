package pw.thedrhax.mosmetro.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import pw.thedrhax.util.Notification;

public class NetworkReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        SharedPreferences.Editor settings_editor = settings.edit();

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if (networkInfo != null &&
                networkInfo.isConnected() &&
                "\"MosMetro_Free\"".equals(wifiInfo.getSSID())) {

            if (!settings.getBoolean("locked", false) && settings.getBoolean("pref_autoconnect", true)) {
                settings_editor.putBoolean("locked", true);
                settings_editor.apply();
                context.startService(new Intent(context, ConnectionService.class));
            }
        } else {
            settings_editor.putBoolean("locked", false);
            settings_editor.apply();
            new Notification(context).setId(0).hide(); // Hide result notification on disconnect
        }
    }
}
