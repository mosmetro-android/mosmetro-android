package pw.thedrhax.mosmetro.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import pw.thedrhax.util.Notification;

public class NetworkReceiver extends BroadcastReceiver {
    private static final String NETWORK_SSID = "\"MosMetro_Free\"";

    public void onReceive(Context context, Intent intent) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();

        if (NETWORK_SSID.equals(info.getSSID())) {
            if (!settings.getBoolean("locked", false) &&
                settings.getBoolean("pref_autoconnect", true)) {

                settings.edit().putBoolean("locked", true).apply();
                context.startService(new Intent(context, ConnectionService.class));
            }
        } else {
            // Try to reconnect the Wi-Fi network
            if (settings.getBoolean("pref_wifi_reconnect", false) &&
                settings.getBoolean("locked", false)) {

                try {
                    for (WifiConfiguration network : manager.getConfiguredNetworks()) {
                        if (network.SSID.equals(NETWORK_SSID)) {
                            manager.enableNetwork(network.networkId, true);
                            manager.reconnect();
                        }
                    }
                } catch (NullPointerException ignored) {}
            }

            settings.edit().putBoolean("locked", false).apply();
            new Notification(context).setId(0).hide(); // Hide result notification on disconnect
        }
    }
}
