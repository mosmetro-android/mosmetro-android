package pw.thedrhax.mosmetro.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import pw.thedrhax.mosmetro.MosMetroConnection;
import pw.thedrhax.util.Util;

public class ConnectionService extends IntentService {
    private SharedPreferences settings;

    public ConnectionService () {
		super("ConnectionService");
	}
	
	public void onCreate() {
		super.onCreate();

        settings = PreferenceManager.getDefaultSharedPreferences(this);
    }

    private static final MosMetroConnection connection = new MosMetroConnection();
	
	public void onHandleIntent(Intent intent) {
        int result = connection.connect();

        if ((result == 0) && (settings.getBoolean("pref_notify", true)))
            Util.notify(this,
                "Успешно подключено",
                "Вы можете отключить уведомления в настройках приложения"
            );
	}
}