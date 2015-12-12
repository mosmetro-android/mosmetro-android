package ru.thedrhax.mosmetro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class NetworkReceiver extends BroadcastReceiver {
    private static boolean lock = false;
    private static FileWriter log_writer;

    public NetworkReceiver() {
        if (log_writer == null) {
            // Open log file
            try {
                File log_file = new File(
                        Environment.getExternalStorageDirectory().getPath() + "/MosMetro_Log.txt"
                );
                if (!log_file.exists()) log_file.createNewFile();

                log_writer = new FileWriter(log_file, true);
            } catch (IOException ignored) {}
        }
    }

    private static final MosMetroConnection connection = new MosMetroConnection() {
        public void log(String message) {
            try {
                log_writer.write(message + "\n");
                log_writer.flush();
            } catch (IOException ignored) {}
        }
    };

    public void onReceive(Context context, Intent intent) {
        WifiInfo info = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);

        if (info == null) {
            lock = false;
            return;
        }

        if (!lock && "\"MosMetro_Free\"".equals(info.getSSID())) {
            lock = true;
            connection.connect();
        }
    }
}
