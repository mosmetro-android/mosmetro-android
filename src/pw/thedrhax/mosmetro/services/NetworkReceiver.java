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

package pw.thedrhax.mosmetro.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

import pw.thedrhax.util.Logger;
import pw.thedrhax.util.WifiUtils;

/**
 * This BroadcastReceiver filters and sends Intents to the ConnectionService.
 *
 * There are two types of Intents accepted by the ConnectionService:
 *     1) Wi-Fi network is definitely connected (startService())
 *     2) No Wi-Fi networks are connected (stopService())
 *
 * NetworkReceiver doesn't take care of:
 *     1) Ignoring duplicated Intents
 *     2) Determining if current SSID is supported by the Provider
 *
 * @see ConnectionService
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 */
public class NetworkReceiver extends BroadcastReceiver {
    private Context context;
    private Intent intent;

    public void onReceive(Context context, Intent intent) {
        this.context = context;
        this.intent = intent;

        // Stop if automatic connection is disabled in settings
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        if (!settings.getBoolean("pref_autoconnect", true))
            return;

        Logger.log(this, "Intent: " + intent.getAction());

        // If Wi-Fi is disabled, stop ConnectionService immediately
        WifiUtils wifi = new WifiUtils(context);
        if (!wifi.isEnabled()) {
            Logger.log(this, "Wi-Fi not enabled");
            stopService();
            return;
        }

        // Listen to all Wi-Fi state changes and start ConnectionService if Wi-Fi is connected
        // This .equals condition is used to allow addition of new Intents in future
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            SupplicantState state = wifi.getWifiInfo(intent).getSupplicantState();
            if (state == null) return;

            Logger.log(this, "SupplicantState: " + state.name());
            switch (state) {
                case COMPLETED:
                case ASSOCIATED: // This appears randomly between multiple CONNECTED states
                    startService();
                    break;
                case DISCONNECTED:
                    stopService();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Start ConnectionService and pass received Intent's content
     */
    private void startService() {
        Logger.log(this, "Starting ConnectionService");
        Intent service = new Intent(context, ConnectionService.class);
        service.setAction(intent.getAction());
        service.putExtras(intent);
        context.startService(service);
    }

    /**
     * Stop ConnectionService
     */
    private void stopService() {
        Logger.log(this, "Stopping ConnectionService");
        context.startService(new Intent(context, ConnectionService.class).setAction("STOP"));
    }
}
