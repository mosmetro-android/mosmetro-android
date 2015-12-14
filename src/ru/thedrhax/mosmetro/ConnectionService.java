package ru.thedrhax.mosmetro;

import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
		connection.connect();
	}
}