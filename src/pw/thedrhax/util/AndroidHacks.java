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
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

public final class AndroidHacks {
    private AndroidHacks() {
    }

    // Bind current process to a specific network
    // Refactored answer from Stack Overflow: http://stackoverflow.com/a/28664841
    public static void bindToWiFi(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < 21)
            return;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        if (!settings.getBoolean("pref_wifi_bind", false))
            return;

        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        for (Network network : cm.getAllNetworks()) {
            NetworkInfo info = cm.getNetworkInfo(network);
            if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI)
                if (info.getState() == NetworkInfo.State.CONNECTED) {
                    if (Build.VERSION.SDK_INT < 23) {
                        try {
                            ConnectivityManager.setProcessDefaultNetwork(network);
                        } catch (IllegalStateException ignored) {
                        }
                    } else {
                        cm.bindProcessToNetwork(network);
                    }
                    break;
                }
        }
    }
}
