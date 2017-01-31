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

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;

public class WifiUtils {
    public static final String UNKNOWN_SSID = "<unknown ssid>";

    private final WifiManager manager;

    public WifiUtils(@NonNull Context context) {
        this.manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    /*
     * Read-only methods
     */

    // Wi-Fi connectivity conditions
    public boolean isConnected(String SSID) {
        if (!manager.isWifiEnabled()) return false;
        if (!getSSID().equalsIgnoreCase(SSID)) return false;
        return true;
    }

    // Clear SSID from platform-specific symbols
    private static String clear (String text) {
        return (text != null && !text.isEmpty()) ? text.replace("\"", "") : UNKNOWN_SSID;
    }

    // Get WifiInfo from Intent or, if not available, from WifiManager
    public WifiInfo getWifiInfo(Intent intent) {
        if (intent != null && Build.VERSION.SDK_INT >= 14) {
            WifiInfo result = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            if (result != null) return result;
        }
        return manager.getConnectionInfo();
    }

    // Get SSID from Intent's EXTRA_WIFI_INFO (API > 14)
    public String getSSID(Intent intent) {
        return clear(getWifiInfo(intent).getSSID());
    }

    // Get SSID directly from WifiManager
    public String getSSID() {
        return getSSID(null);
    }

    // Get current IP from WifiManager
    public int getIP() {
        return manager.getConnectionInfo().getIpAddress();
    }

    // Get main Wi-Fi state
    public boolean isEnabled() {
        return manager.isWifiEnabled();
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
