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

    /*
     * Read-only methods
     */

    public boolean isEnabled() {
        return manager.isWifiEnabled();
    }

    // Clear SSID from platform-specific symbols
    private static String clear (String text) {
        return (text != null && !text.isEmpty()) ? text.replace("\"", "") : UNKNOWN_SSID;
    }

    // Get SSID directly from WifiManager
    public String getSSID() {
        return clear(manager.getConnectionInfo().getSSID());
    }

    // Get SSID from Intent's EXTRA_WIFI_INFO (API > 14)
    public String getSSID(Intent intent) {
        if (intent == null) return getSSID();

        // Get SSID from Intent
        if (Build.VERSION.SDK_INT >= 14) {
            WifiInfo info = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            if (info != null)
                return clear(info.getSSID());
        }

        return getSSID();
    }

    // Get current IP from WifiManager
    public int getIP() {
        return manager.getConnectionInfo().getIpAddress();
    }

    /*
     * Control methods
     */

    // Reconnect to SSID (only if already configured)
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
}
