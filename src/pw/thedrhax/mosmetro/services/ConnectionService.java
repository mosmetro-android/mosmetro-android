package pw.thedrhax.mosmetro.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import pw.thedrhax.mosmetro.MosMetroConnection;

import java.util.Calendar;

public class ConnectionService extends IntentService {
	public ConnectionService () {
		super("ConnectionService");
	}
	
	public void onCreate() {
		super.onCreate();
    }

    private static final MosMetroConnection connection = new MosMetroConnection();
	
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