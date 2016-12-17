package pw.thedrhax.util;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

public class WifiUtils {
    public static final String UNKNOWN_SSID = "<unknown ssid>";

    private WifiManager manager;

    public WifiUtils(Context context) {
        this.manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public static String clear (String text) {
        return (text != null && !text.isEmpty()) ? text.replace("\"", "") : UNKNOWN_SSID;
    }

    public String get() {
        return clear(manager.getConnectionInfo().getSSID());
    }

    public String get (Intent intent) {
        if (intent == null) return get();

        // Get SSID from Intent
        if (Build.VERSION.SDK_INT >= 14) {
            WifiInfo info = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            if (info != null)
                return clear(info.getSSID());
        }

        return get();
    }

    public void reconnect(String SSID) {
        try {
            for (WifiConfiguration network : manager.getConfiguredNetworks()) {
                if (clear(network.SSID).equals(SSID)) {
                    manager.enableNetwork(network.networkId, true);
                    manager.reassociate();
                }
            }
        } catch (NullPointerException ignored) {}
    }

    public int getIP() {
        return manager.getConnectionInfo().getIpAddress();
    }

    public boolean isEnabled() {
        return manager.isWifiEnabled();
    }
}
