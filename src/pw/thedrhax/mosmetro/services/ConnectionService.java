package pw.thedrhax.mosmetro.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.MainActivity;
import pw.thedrhax.mosmetro.activities.SettingsActivity;
import pw.thedrhax.mosmetro.authenticator.Authenticator;
import pw.thedrhax.mosmetro.authenticator.AuthenticatorStat;
import pw.thedrhax.util.Notification;

public class ConnectionService extends IntentService {
    private SharedPreferences settings;

    // Preferences
    private boolean pref_notify_progress;
    private int pref_retry_count;
    private int pref_retry_delay;

    // Authenticator
    private Authenticator connection;

    // Notifications
    private Notification notify_progress;
    private Notification notification;

    public ConnectionService () {
		super("ConnectionService");
	}
	
	@Override
    public void onCreate() {
		super.onCreate();

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        pref_notify_progress = settings.getBoolean("pref_notify_progress", true) && (Build.VERSION.SDK_INT >= 14);
        pref_retry_count = Integer.parseInt(settings.getString("pref_retry_count", "5"));
        pref_retry_delay = Integer.parseInt(settings.getString("pref_retry_delay", "10"));

        connection = new AuthenticatorStat(this, true) {
            public void onChangeProgress(int progress) {
                if (pref_notify_progress) {
                    notify_progress.setProgress(progress);
                    notify_progress.show();
                }
            }
        };

        notify_progress = new Notification(this)
                .setTitle("Подключение к MosMetro_Free")
                .setIcon(R.drawable.ic_notification_connecting)
                .setId(1);

        notification = new Notification(this)
                .setId(0);
    }

    private void notify (int result) {
        switch (result) {
            // Successful connection
            case 0:
            // Already connected
            case 1:
                if (settings.getBoolean("pref_notify_success", true))
                    notification
                            .setTitle("Успешно подключено")
                            .setText("Нажмите, чтобы открыть настройки уведомлений")
                            .setCancellable(false)
                            .setIntent(new Intent(this, SettingsActivity.class))
                            .show();
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
                            .setText("Нажмите, чтобы узнать подробности или попробовать снова")
                            .setIcon(R.drawable.ic_notification_error)
                            .setIntent(debug)
                            .show();
                }
        }
    }

    private int connect() {
        int result, count = 0;

        do {
            if (count > 0) {
                if (pref_notify_progress)
                    notify_progress.setText(
                            "Ожидание... (попытка " + (count+1) + " из " + pref_retry_count + ")"
                    ).setContinuous().show();

                try {
                    Thread.sleep(pref_retry_delay * 1000);
                } catch (InterruptedException ignored) {}
            }

            if (pref_notify_progress)
                notify_progress.setText(
                        "Подключаюсь... (попытка " + (count+1) + " из " + pref_retry_count + ")"
                ).show();

            result = connection.connect();

            if (!settings.getBoolean("locked", false)) {
                connection.log("Ошибка: соединение с сетью прервалось (поезд уехал?)");
                break;
            }

            count++;
        } while (count < pref_retry_count && result > 1);

        notify_progress.hide();

        return result;
    }
	
	public void onHandleIntent(Intent intent) {
        int result = connect();

        // Show notification only if locked (connected to Wi-Fi)
        if (settings.getBoolean("locked", false))
            notify(result);
	}
}