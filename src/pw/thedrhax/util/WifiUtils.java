package pw.thedrhax.util;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

public class WifiUtils {
    private WifiManager manager;

    public WifiUtils(Context context) {
        this.manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public static String clear (String text) {
        return text.replace("\"", "");
    }

    public String get() {
        return clear(manager.getConnectionInfo().getSSID());
    }

    public String get (Intent intent) {
        if (intent == null) return get();

        // Get SSID from Intent
        if (Build.VERSION.SDK_INT >= 14) {
            WifiInfo info = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            if (info != null) return info.getSSID().replace("\"", "");
        }

        return get();
    }
}
