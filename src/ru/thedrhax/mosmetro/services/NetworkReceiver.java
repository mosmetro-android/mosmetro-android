package ru.thedrhax.mosmetro.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class NetworkReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        WifiInfo info = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);

        if (info == null) return;

        if ("\"MosMetro_Free\"".equals(info.getSSID()))
            context.startService(new Intent(context, ConnectionService.class));
    }
}
