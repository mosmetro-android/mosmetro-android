package pw.thedrhax.mosmetro.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import pw.thedrhax.mosmetro.activities.MainActivity;
import pw.thedrhax.mosmetro.activities.SettingsActivity;
import pw.thedrhax.mosmetro.authenticator.Authenticator;
import pw.thedrhax.mosmetro.authenticator.AuthenticatorStat;
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
        int result = 0, count = 0;

        // Retry 5 times, wait 1 minute after each retry, stop on error
        while (settings.getBoolean("locked", false) && count < 5 && result < 3) {
            result = connection.connect();
            count++;

            switch (result) {
                // Successful connection
                case 0:
                    if (settings.getBoolean("pref_notify_success", true))
                        Util.notify(this,
                                "Успешно подключено",
                                "Нажмите, чтобы открыть настройки уведомлений",
                                new Intent(this, SettingsActivity.class)
                        );
                    break;
                // Already connected: ok too
                case 1:
                    break;
                // Wrong network
                case 2:
                    break;
                // Error
                case 3:
                    if (settings.getBoolean("pref_notify_fail", true)) {
                        Intent debug = new Intent(this, MainActivity.class);
                        debug.putExtra("log", connection.getLog());
                        debug.putExtra("debug", connection.getDebug());

                        Util.notify(this,
                                "Не удалось подключиться",
                                "Нажмите, чтобы увидеть лог",
                                debug
                        );
                    }
                    break;
            }

            // Wait 1 minute
            if (count < 5) {
                connection.log("Повторная попытка (" + (count+1) + " из 5) через 60 секунд");
                try {
                    Thread.sleep(60 * 1000);
                } catch (InterruptedException ignored) {}
            }
        }
	}
}