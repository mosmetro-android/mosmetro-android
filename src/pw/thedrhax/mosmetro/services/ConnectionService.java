package pw.thedrhax.mosmetro.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import pw.thedrhax.mosmetro.MosMetroConnection;

import java.util.Calendar;

public class ConnectionService extends IntentService {
    private SharedPreferences lock;
    private SharedPreferences.Editor editor;

	public ConnectionService () {
		super("ConnectionService");
	}
	
	public void onCreate() {
		super.onCreate();

        lock = getSharedPreferences("MosMetro_Lock", 0);
        editor = lock.edit();
    }

    private static final MosMetroConnection connection = new MosMetroConnection();
	
	public void onHandleIntent(Intent intent) {
        Long time = Calendar.getInstance().getTimeInMillis();
        Long lastSuccess = lock.getLong("LastSuccess", 0);

        if (time < lastSuccess + 60*1000) return;

		if (connection.connect()) {
            time = Calendar.getInstance().getTimeInMillis();
            editor.putLong("LastSuccess", time);
            editor.apply();
        }
	}
}