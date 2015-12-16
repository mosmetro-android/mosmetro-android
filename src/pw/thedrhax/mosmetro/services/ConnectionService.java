package pw.thedrhax.mosmetro.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import pw.thedrhax.mosmetro.MosMetroConnection;
import pw.thedrhax.util.Util;

import java.util.Calendar;

public class ConnectionService extends IntentService {
    private SharedPreferences settings;
    private SharedPreferences lock;

    public ConnectionService () {
		super("ConnectionService");
	}
	
	public void onCreate() {
		super.onCreate();

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        lock = getSharedPreferences("MosMetro_Lock", 0);
    }

    private static final MosMetroConnection connection = new MosMetroConnection();
	
	public void onHandleIntent(Intent intent) {
        Long time = Calendar.getInstance().getTimeInMillis();
        Long lastSuccess = lock.getLong("LastSuccess", 0);

        if (time < lastSuccess + 60*1000) return;

        int result = connection.connect();

		if (result < 2) {
            time = Calendar.getInstance().getTimeInMillis();

            SharedPreferences.Editor editor = lock.edit();
            editor.putLong("LastSuccess", time);
            editor.apply();
        }

        if ((result == 0) && (settings.getBoolean("pref_notify", true)))
            Util.notify(this,
                "Успешно подключено",
                "Вы можете отключить уведомления в настройках приложения"
            );
	}
}