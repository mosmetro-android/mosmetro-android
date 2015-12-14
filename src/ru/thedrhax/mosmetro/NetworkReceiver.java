package ru.thedrhax.mosmetro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class NetworkReceiver extends BroadcastReceiver {
    private static boolean lock = false;

    public void onReceive(Context context, Intent intent) {
        WifiInfo info = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);

        if (info == null) {
            lock = false;
            return;
        }

        if (!lock && "\"MosMetro_Free\"".equals(info.getSSID())) {
            lock = true;
            context.startService(new Intent(context, ConnectionService.class));
        }
    }
}
