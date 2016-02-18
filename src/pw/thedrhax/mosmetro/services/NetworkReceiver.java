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

import java.util.List;

public class NetworkReceiver extends BroadcastReceiver {
    private static final String NETWORK_SSID = "\"MosMetro_Free\"";

    public void onReceive(Context context, Intent intent) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor settings_editor = settings.edit();

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if (NETWORK_SSID.equals(wifiInfo.getSSID())) {
            if (!settings.getBoolean("locked", false) && settings.getBoolean("pref_autoconnect", true)) {
                settings_editor.putBoolean("locked", true);
                settings_editor.apply();
                context.startService(new Intent(context, ConnectionService.class));
            }
        } else {
            // Try to reconnect the Wi-Fi network
            if (settings.getBoolean("pref_wifi_reconnect", false) &&
                settings.getBoolean("locked", false)) {

                List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
                if (list != null)
                    for (WifiConfiguration network : list) {
                        if (network.SSID.equals(NETWORK_SSID)) {
                            wifiManager.enableNetwork(network.networkId, true);
                            wifiManager.reconnect();
                        }
                    }
            }

            settings_editor.putBoolean("locked", false);
            settings_editor.apply();
            new Notification(context).setId(0).hide(); // Hide result notification on disconnect
        }
    }
}
