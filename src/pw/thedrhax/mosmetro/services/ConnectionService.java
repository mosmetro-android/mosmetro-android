package pw.thedrhax.mosmetro.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
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
    private static final String NETWORK_SSID = "\"MosMetro_Free\"";

    // Preferences
    private WifiManager manager;
    private SharedPreferences settings;
    private int pref_retry_count;
    private int pref_retry_delay;
    private int pref_ip_wait;
    private boolean pref_colored_icons;

    // Notifications
    private Notification notify_progress;
    private Notification notification;

    // Authenticator
    private Logger logger;
    private Authenticator connection;

    public ConnectionService () {
		super("ConnectionService");
	}
	
	@Override
    public void onCreate() {
		super.onCreate();

        manager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        pref_retry_count = Integer.parseInt(settings.getString("pref_retry_count", "3"));
        pref_retry_delay = Integer.parseInt(settings.getString("pref_retry_delay", "5"));
        pref_ip_wait = Integer.parseInt(settings.getString("pref_ip_wait", "0"));
        pref_colored_icons = (Build.VERSION.SDK_INT <= 20) || settings.getBoolean("pref_notify_alternative", false);

        notify_progress = new Notification(this)
                .setTitle("Подключение к MosMetro_Free")
                .setIcon(pref_colored_icons ?
                        R.drawable.ic_notification_connecting_colored :
                        R.drawable.ic_notification_connecting)
                .setId(1)
                .setCancellable(false)
                .setEnabled(settings.getBoolean("pref_notify_progress", true) && (Build.VERSION.SDK_INT >= 14));

        notification = new Notification(this)
                .setId(0);

        logger = new Logger();
        connection = new AuthenticatorStat(this, true) {
            public void onChangeProgress(int progress) {
                notify_progress
                        .setProgress(progress)
                        .show();
            }
        };
        connection.setLogger(logger);
    }

    private void notify (int result) {
        switch (result) {
            case Authenticator.STATUS_CONNECTED:
            case Authenticator.STATUS_ALREADY_CONNECTED:
                notification
                        .setTitle("Успешно подключено")
                        .setIcon(pref_colored_icons ?
                                R.drawable.ic_notification_success_colored :
                                R.drawable.ic_notification_success);

                if (settings.getBoolean("pref_notify_success_log", false)) {
                    notification
                            .setText("Нажмите, чтобы узнать подробности")
                            .setIntent(new Intent(this, DebugActivity.class).putExtra("logger", logger));
                } else {
                    notification
                            .setText("Нажмите, чтобы открыть настройки уведомлений")
                            .setIntent(new Intent(this, SettingsActivity.class));
                }

                notification
                        .setCancellable(!settings.getBoolean("pref_notify_success_lock", false))
                        .setEnabled(settings.getBoolean("pref_notify_success", true))
                        .show();

                return;

            case Authenticator.STATUS_NOT_REGISTERED:
                notification
                        .setTitle("Устройство не зарегистрировано")
                        .setText("Нажмите, чтобы перейти к регистрации")
                        .setIcon(pref_colored_icons ?
                                R.drawable.ic_notification_register_colored :
                                R.drawable.ic_notification_register)
                        .setIntent(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://vmet.ro")))
                        .setEnabled(settings.getBoolean("pref_notify_fail", true))
                        .show();

                return;

            case Authenticator.STATUS_ERROR:
                notification
                        .setTitle("Не удалось подключиться")
                        .setText("Нажмите, чтобы узнать подробности или попробовать снова")
                        .setIcon(pref_colored_icons ?
                                R.drawable.ic_notification_error_colored :
                                R.drawable.ic_notification_error)
                        .setIntent(new Intent(this, DebugActivity.class).putExtra("logger", logger))
                        .setEnabled(settings.getBoolean("pref_notify_fail", true))
                        .show();
        }
    }

    private boolean isWifiConnected() {
        return NETWORK_SSID.equals(manager.getConnectionInfo().getSSID());
    }

    private int connect() {
        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        int result, count = 0;

        logger.log_debug(">> Ожидание получения IP адреса...");
        notify_progress
                    .setText("Ожидание получения IP адреса...")
                    .setContinuous()
                    .show();
        while (manager.getConnectionInfo().getIpAddress() == 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}

            if (!isWifiConnected()) {
                logger.log_debug("< Ошибка: Соединение с сетью прервалось");

                logger.log("\nВозможные причины:");
                logger.log(" * Вы отключились от сети MosMetro_Free");
                logger.log(" * Поезд, с которым устанавливалось соединение, уехал");
                logger.log(" * Точка доступа в поезде отключилась");

                return Authenticator.STATUS_ERROR;
            }

            if (pref_ip_wait != 0 && count++ == pref_ip_wait) {
                logger.log_debug("<< Ошибка: IP адрес не был получен в течение " + pref_ip_wait + " секунд");

                logger.log("\nВозможные причины:");
                logger.log(" * Устройство не полностью подключилось к сети: убедитесь, что статус сети \"Подключено\"");
                logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");

                return Authenticator.STATUS_ERROR;
            }
        }
        logger.log_debug("<< IP адрес получен в течение " + count/2 + " секунд");

        count = 0;
        do {
            if (count > 0) {
                notify_progress
                        .setText("Ожидание... (попытка " + (count+1) + " из " + pref_retry_count + ")")
                        .setContinuous()
                        .show();

                try {
                    Thread.sleep(pref_retry_delay * 1000);
                } catch (InterruptedException ignored) {}
            }

            notify_progress
                    .setText("Подключаюсь... (попытка " + (count+1) + " из " + pref_retry_count + ")")
                    .show();

            result = connection.connect();

            if (!isWifiConnected()) {
                logger.log_debug("< Ошибка: Соединение с сетью прервалось");

                logger.log("\nВозможные причины:");
                logger.log(" * Вы отключились от сети MosMetro_Free");
                logger.log(" * Поезд, с которым устанавливалось соединение, уехал");
                logger.log(" * Точка доступа в поезде отключилась");

                return Authenticator.STATUS_ERROR;
            }
        } while (count++ < pref_retry_count && result > Authenticator.STATUS_ALREADY_CONNECTED);

        return result;
    }
	
	public void onHandleIntent(Intent intent) {
        if (isWifiConnected()) {
            if (!settings.getBoolean("locked", false)) {
                logger.date("> ", "");
                int result = connect();
                logger.date("< ", "\n");
                settings.edit().putBoolean("locked", true).apply();

                // Remove progress notification
                notify_progress.hide();

                // Show notification only if locked (connected to Wi-Fi)
                if (isWifiConnected()) notify(result);
            }
        } else {
            // Try to reconnect the Wi-Fi network
            if (settings.getBoolean("pref_wifi_reconnect", false) &&
                settings.getBoolean("locked", false)) {
                try {
                    for (WifiConfiguration network : manager.getConfiguredNetworks()) {
                        if (network.SSID.equals(NETWORK_SSID)) {
                            manager.enableNetwork(network.networkId, true);
                            manager.reconnect();
                        }
                    }
                } catch (NullPointerException ignored) {}
            }
            settings.edit().putBoolean("locked", false).apply();
            notification.hide();
        }
	}
}