package pw.thedrhax.mosmetro.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import pw.thedrhax.mosmetro.authenticator.Authenticator;
import pw.thedrhax.mosmetro.authenticator.AuthenticatorStat;
import pw.thedrhax.mosmetro.activities.DebugActivity;
import pw.thedrhax.mosmetro.activities.SettingsActivity;
import pw.thedrhax.util.Util;

public class ConnectionService extends IntentService {
    private SharedPreferences settings;

    public ConnectionService () {
		super("ConnectionService");
	}

    private Authenticator connection;
	
	public void onCreate() {
		super.onCreate();

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        connection = new AuthenticatorStat(this, true);
    }
	
	public void onHandleIntent(Intent intent) {
        switch(connection.connect()) {
            case 0:
                if (settings.getBoolean("pref_notify_success", true))
                    Util.notify(this,
                            "Успешно подключено",
                            "Нажмите, чтобы открыть настройки уведомлений",
                            new Intent(this, SettingsActivity.class)
                    );
                break;

            case 1:
                break;

            case 2:
                if (settings.getBoolean("pref_notify_fail", true)) {
                    Intent debug = new Intent(this, DebugActivity.class);
                    debug.putExtra("log", connection.getLog());
                    
                    Util.notify(this,
                            "Не удалось подключиться",
                            "Нажмите, чтобы увидеть лог",
                            debug
                    );
                }
                break;
       	}
	}
}