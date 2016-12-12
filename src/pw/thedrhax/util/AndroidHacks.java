package pw.thedrhax.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;

public class AndroidHacks {

    // Bind current process to a specific network
    // Refactored answer from Stack Overflow: http://stackoverflow.com/a/28664841
    public static void bindToWiFi(Context context) {
        if (Build.VERSION.SDK_INT < 21)
            return;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        if (!settings.getBoolean("pref_wifi_bind", false))
            return;

        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        for (Network i : cm.getAllNetworks()) {
            NetworkInfo info = cm.getNetworkInfo(i);
            if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI)
                if (info.getState() == NetworkInfo.State.CONNECTED) {
                    if (Build.VERSION.SDK_INT < 23)
                        try {
                            ConnectivityManager.setProcessDefaultNetwork(i);
                        } catch (IllegalStateException ignored) {}
                    else
                        cm.bindProcessToNetwork(i);
                    break;
                }
        }
    }
}
