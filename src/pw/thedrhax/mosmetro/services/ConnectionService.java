package pw.thedrhax.mosmetro.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.DebugActivity;
import pw.thedrhax.mosmetro.activities.SettingsActivity;
import pw.thedrhax.mosmetro.authenticator.Authenticator;
import pw.thedrhax.mosmetro.authenticator.Chooser;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Notification;

public class ConnectionService extends IntentService {
    private static boolean running = false;

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
                .setIcon(pref_colored_icons ?
                        R.drawable.ic_notification_connecting_colored :
                        R.drawable.ic_notification_connecting)
                .setId(1)
                .setCancellable(false)
                .setEnabled(settings.getBoolean("pref_notify_progress", true) && (Build.VERSION.SDK_INT >= 14));

        notification = new Notification(this)
                .setId(0);

        logger = new Logger();
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
                        .setIntent(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://wi-fi.ru")))
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
        WifiInfo info = manager.getConnectionInfo();

        // Strict check by supplicant state
        if (settings.getBoolean("pref_autoconnect_strict", false))
            if (!info.getSupplicantState().equals(SupplicantState.COMPLETED))
                return false;

        if (connection == null) {
            for (Class<? extends Authenticator> authenticator : Authenticator.SUPPORTED_NETWORKS) {
                try {
                    if (info.getSSID().equals(authenticator.getField("SSID").get(authenticator)))
                        return true;
                } catch (Exception ignored) {}
            }
        } else {
            if (info.getSSID().equals(connection.getSSID()))
                return true;
        }

        return false;
    }

    private boolean waitForIP() {
        int count = 0;

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
                return false;
            }

            if (pref_ip_wait != 0 && count++ == pref_ip_wait) {
                logger.log_debug("<< Ошибка: IP адрес не был получен в течение " + pref_ip_wait + " секунд");
                return false;
            }
        }

        logger.log_debug("<< IP адрес получен в течение " + count/2 + " секунд");
        return true;
    }

    private int connect() {
        int result, count = 0;

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

            result = connection.start();

            if (!isWifiConnected()) {
                logger.log_debug("< Ошибка: Соединение с сетью прервалось");
                result = Authenticator.STATUS_ERROR; break;
            }
        } while (++count < pref_retry_count && result > Authenticator.STATUS_ALREADY_CONNECTED);

        // Remove progress notification
        notify_progress.hide();

        return result;
    }
	
	public void onHandleIntent(Intent intent) {
        logger.date("> ", "");

        // Check if Wi-Fi is connected
        if (!isWifiConnected()) return;

        // Disable calls from the NetworkReceiver
        running = true;

        connection = new Chooser(this, true, logger).choose();
        if (connection == null) return;

        connection.setLogger(logger);
        connection.setProgressListener(new Authenticator.ProgressListener() {
            @Override
            public void onProgressUpdate(int progress) {
                notify_progress
                        .setProgress(progress)
                        .show();
            }
        });

        notify_progress.setTitle("Подключение к " + connection.getSSID());

        // Try to connect
        int result = Authenticator.STATUS_ERROR;
        if (waitForIP()) result = connect();

        logger.date("< ", "\n");

        // Notify user if still connected to Wi-Fi
        if (isWifiConnected()) notify(result);

        // Wait until Wi-Fi is disconnected
        int count = 0;
        while (isWifiConnected()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}

            // Check internet connection each 5 seconds
            if (++count == 5*2 && connection.isConnected() != Authenticator.CHECK_CONNECTED) {
                startService(new Intent(this, ConnectionService.class)); // Restart this service
                break;
            }
        }

        notification.hide();

        // Try to reconnect the Wi-Fi network
        if (settings.getBoolean("pref_wifi_reconnect", false)) {
            try {
                for (WifiConfiguration network : manager.getConfiguredNetworks()) {
                    if (network.SSID.equals(connection.getSSID())) {
                        manager.enableNetwork(network.networkId, true);
                        manager.reconnect();
                    }
                }
            } catch (NullPointerException ignored) {}
        }

        running = false;
	}
	
	@Override
    public void onDestroy() {
        running = false;
        notification.hide();
        notify_progress.hide();
    }

    public static boolean isRunning() {
        return running;
    }
}
