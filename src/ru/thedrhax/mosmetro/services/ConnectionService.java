package ru.thedrhax.mosmetro.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import ru.thedrhax.mosmetro.MosMetroConnection;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

public class ConnectionService extends IntentService {
	private static FileWriter log_writer;
	
	public ConnectionService () {
		super("ConnectionService");
	}
	
	public void onCreate() {
		super.onCreate();

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
	
	public void onHandleIntent(Intent intent) {
        SharedPreferences settings = getSharedPreferences("MosMetro_Lock", 0);

        Long time = Calendar.getInstance().getTimeInMillis();
        Long lastSuccess = settings.getLong("LastSuccess", 0);

        if (time < lastSuccess + 5*60*1000) return;

        SharedPreferences.Editor editor = settings.edit();

		if (connection.connect()) {
            time = Calendar.getInstance().getTimeInMillis();
            editor.putLong("LastSuccess", time);
            editor.apply();
        }
	}
}