package pw.thedrhax.mosmetro.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import pw.thedrhax.mosmetro.activities.MainActivity;
import pw.thedrhax.mosmetro.activities.SettingsActivity;
import pw.thedrhax.mosmetro.authenticator.Authenticator;
import pw.thedrhax.mosmetro.authenticator.AuthenticatorStat;
import pw.thedrhax.util.Notification;

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
        Notification progress = new Notification(this)
                .setTitle("Подключение...")
                .setText("Пожалуйта, подождите...")
                .setContinuous();

        boolean pref_notify_progress = settings.getBoolean("pref_notify_progress", true);
        int pref_retry_count = Integer.parseInt(settings.getString("pref_retry_count", "5"));
        int pref_retry_delay = Integer.parseInt(settings.getString("pref_retry_delay", "10"));

        int result, count = 0;

        if (pref_notify_progress) progress.show();
        do {
            // Wait 1 minute
            if (count > 0) {
                if (count < pref_retry_count)
                    connection.log(
                            "Повторная попытка (" + (count+1) +
                            " из " + pref_retry_count + ") через "
                            + pref_retry_delay + " секунд\n"
                    );

                try {
                    Thread.sleep(pref_retry_delay * 1000);
                } catch (InterruptedException ignored) {}

                if (pref_notify_progress)
                    progress.setText("Попытка " + (count+1) + " из " + pref_retry_count + "...").show();
            }

            result = connection.connect();

            if (!settings.getBoolean("locked", false)) {
                connection.log("Ошибка: соединение с сетью прервалось (поезд уехал?)");
                break;
            }

            count++;
        } while (count < pref_retry_count && result > 1);

        /*
         * Notify user about result
         */

        Notification notification = new Notification(this);

        switch (result) {
            // Successful connection
            case 0:
                if (settings.getBoolean("pref_notify_success", true))
                    notification
                            .setTitle("Успешно подключено")
                            .setText("Нажмите, чтобы открыть настройки уведомлений")
                            .setIntent(new Intent(this, SettingsActivity.class))
                            .show();
            // Already connected
            case 1:
                if (pref_notify_progress) progress.hide();
                return;
            // Wrong network
            case 2:
            // Error
            case 3:
                if (settings.getBoolean("pref_notify_fail", true)) {
                    Intent debug = new Intent(this, MainActivity.class);
                    debug.putExtra("log", connection.getLog());
                    debug.putExtra("debug", connection.getDebug());

                    notification
                            .setTitle("Не удалось подключиться")
                            .setText("Нажмите, чтобы увидеть лог")
                            .setIntent(debug)
                            .show();
                }
        }
	}
}