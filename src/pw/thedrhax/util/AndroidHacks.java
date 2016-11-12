package pw.thedrhax.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;

public class AndroidHacks {

    // Bind current process to a specific network
    // Refactored answer from Stack Overflow: http://stackoverflow.com/a/28664841
    public static void bindToWiFi(Context context) {
        if (Build.VERSION.SDK_INT < 21)
            return;

        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        for (Network i : cm.getAllNetworks()) {
            NetworkInfo info = cm.getNetworkInfo(i);
            if (info.getType() == ConnectivityManager.TYPE_WIFI)
                if (info.getState() == NetworkInfo.State.CONNECTED) {
                    cm.bindProcessToNetwork(i);
                    break;
                }
        }
    }
}
