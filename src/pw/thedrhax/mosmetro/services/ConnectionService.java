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
        int result, count = 0;

        // Retry 5 times, wait 1 minute after each retry
        do {
            // Wait 1 minute
            if (count > 0) {
                if (count < 5) connection.log("Повторная попытка (" + (count+1) + " из 5) через 60 секунд");
                try {
                    Thread.sleep(60 * 1000);
                } catch (InterruptedException ignored) {}
            }

            result = connection.connect();

            if (!settings.getBoolean("locked", false)) {
                connection.log("Ошибка: соединение с сетью прервалось (поезд уехал?)");
                break;
            }

            count++;
        } while (count < 5 && result > 1); // Repeat until internet is available or 5 tries are made

        /*
         * Notify user about result
         */

        switch (result) {
            // Successful connection
            case 0:
                if (settings.getBoolean("pref_notify_success", true))
                    Util.notify(this,
                            "Успешно подключено",
                            "Нажмите, чтобы открыть настройки уведомлений",
                            new Intent(this, SettingsActivity.class)
                    );
            // Already connected
            case 1:
                return;
            // Wrong network
            case 2:
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
        }
	}
}