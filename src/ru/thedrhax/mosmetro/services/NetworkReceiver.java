package ru.thedrhax.mosmetro.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

public class NetworkReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!preferences.getBoolean("pref_autoconnect", true)) return;

        WifiInfo info = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);

        if (info == null) return;

        if ("\"MosMetro_Free\"".equals(info.getSSID()))
            context.startService(new Intent(context, ConnectionService.class));
    }
}
