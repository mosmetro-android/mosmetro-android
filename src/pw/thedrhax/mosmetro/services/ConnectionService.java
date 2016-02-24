package pw.thedrhax.mosmetro.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.DebugActivity;
import pw.thedrhax.mosmetro.activities.SettingsActivity;
import pw.thedrhax.mosmetro.authenticator.Authenticator;
import pw.thedrhax.mosmetro.authenticator.AuthenticatorStat;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Notification;

public class ConnectionService extends IntentService {
    private SharedPreferences settings;

    // Preferences
    private boolean pref_notify_progress;
    private int pref_retry_count;
    private int pref_retry_delay;

    // Logger
    private Logger logger;

    // Authenticator
    private Authenticator connection;

    // Notifications
    private Notification notify_progress;
    private Notification notification;
    boolean colored_icons;

    public ConnectionService () {
		super("ConnectionService");
	}
	
	@Override
    public void onCreate() {
		super.onCreate();

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        pref_notify_progress = settings.getBoolean("pref_notify_progress", true) && (Build.VERSION.SDK_INT >= 14);
        pref_retry_count = Integer.parseInt(settings.getString("pref_retry_count", "3"));
        pref_retry_delay = Integer.parseInt(settings.getString("pref_retry_delay", "5"));

        logger = new Logger();

        connection = new AuthenticatorStat(this, true) {
            public void onChangeProgress(int progress) {
                if (pref_notify_progress) {
                    notify_progress.setProgress(progress);
                    notify_progress.show();
                }
            }
        };

        // Set logger
        connection.setLogger(logger);

        colored_icons = (Build.VERSION.SDK_INT <= 20) || settings.getBoolean("pref_notify_alternative", false);

        notify_progress = new Notification(this)
                .setTitle("Подключение к MosMetro_Free")
                .setIcon(colored_icons ?
                        R.drawable.ic_notification_connecting_colored :
                        R.drawable.ic_notification_connecting)
                .setId(1)
                .setCancellable(false);

        notification = new Notification(this)
                .setId(0);
    }
    
    private Intent debugIntent (Logger logger) {
        Intent debug = new Intent(this, DebugActivity.class);
        debug.putExtra("log", logger.getLog());
        debug.putExtra("debug", logger.getDebug());
        debug.putExtra("ConnectionService", true);
        return debug;
    }

    private void notify (int result) {
        switch (result) {
            case Authenticator.STATUS_CONNECTED:
            case Authenticator.STATUS_ALREADY_CONNECTED:
                if (settings.getBoolean("pref_notify_success", true)) {
                    notification
                            .setTitle("Успешно подключено")
                            .setIcon(colored_icons ?
                                    R.drawable.ic_notification_success_colored :
                                    R.drawable.ic_notification_success);
                            
                    if (settings.getBoolean("pref_notify_success_log", false)) {
                        notification
                                .setText("Нажмите, чтобы узнать подробности")
                                .setIntent(debugIntent(logger));
                    } else {
                        notification
                                .setText("Нажмите, чтобы открыть настройки уведомлений")
                                .setIntent(new Intent(this, SettingsActivity.class));
                    }
                    notification.show();
                }
                return;
            case Authenticator.STATUS_ERROR:
                if (settings.getBoolean("pref_notify_fail", true))
                    notification
                            .setTitle("Не удалось подключиться")
                            .setText("Нажмите, чтобы узнать подробности или попробовать снова")
                            .setIcon(colored_icons ?
                                    R.drawable.ic_notification_error_colored :
                                    R.drawable.ic_notification_error)
                            .setIntent(debugIntent(logger))
                            .show();
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
                logger.log_debug("Ошибка: соединение с сетью прервалось");

                logger.log("\nВозможные причины:");
                logger.log(" * Вы отключились от сети MosMetro_Free");
                logger.log(" * Поезд, с которым устанавливалось соединение, уехал");
                logger.log(" * Точка доступа в поезде отключилась");
                break;
            }

            count++;
        } while (count < pref_retry_count && result > Authenticator.STATUS_ALREADY_CONNECTED);

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