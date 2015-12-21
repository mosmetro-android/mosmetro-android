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
        switch(connection.connect()) {
            case 0:
                if (settings.getBoolean("pref_notify_success", true))
                    Util.notify(this,
                            "Успешно подключено",
                            "Вы можете отключить уведомления в настройках приложения"
                    );
                break;

            case 1:
                break;

            case 2:
                if (settings.getBoolean("pref_notify_fail", true))
                    Util.notify(this,
                            "Не удалось подключиться",
                            "Попробуйте ручной режим или дождитесь повторной попытки"
                    );
                break;
       	}
	}
}