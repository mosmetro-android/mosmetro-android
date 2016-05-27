package pw.thedrhax.mosmetro.services;

import android.app.IntentService;
import android.app.PendingIntent;
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
    private boolean from_shortcut = false;

    // Preferences
    private WifiManager manager;
    private SharedPreferences settings;
    private int pref_retry_count;
    private int pref_retry_delay;
    private int pref_ip_wait;
    private boolean pref_colored_icons;
    private boolean pref_notify_success_lock;

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
        pref_ip_wait = Integer.parseInt(settings.getString("pref_ip_wait", "30"));
        pref_colored_icons = (Build.VERSION.SDK_INT <= 20) || settings.getBoolean("pref_notify_alternative", false);
        pref_notify_success_lock = settings.getBoolean("pref_notify_success_lock", false);

        notify_progress = new Notification(this)
                .setIcon(pref_colored_icons ?
                        R.drawable.ic_notification_connecting_colored :
                        R.drawable.ic_notification_connecting)
                .setId(1)
                .setEnabled(settings.getBoolean("pref_notify_progress", true) && (Build.VERSION.SDK_INT >= 14))
                .setDeleteIntent(PendingIntent.getService(
                        this, 0,
                        new Intent(this, ConnectionService.class).setAction("STOP"),
                        PendingIntent.FLAG_UPDATE_CURRENT)
                );

        notification = new Notification(this)
                .setId(0);

        logger = new Logger();
    }

    private void notify (int result) {
        if (!running) return;

        switch (result) {
            case Authenticator.STATUS_CONNECTED:
            case Authenticator.STATUS_ALREADY_CONNECTED:
                notification
                        .setTitle(getString(R.string.notification_success))
                        .setIcon(pref_colored_icons ?
                                R.drawable.ic_notification_success_colored :
                                R.drawable.ic_notification_success);

                if (settings.getBoolean("pref_notify_success_log", false)) {
                    notification
                            .setText(getString(R.string.notification_success_log))
                            .setIntent(new Intent(this, DebugActivity.class).putExtra("logger", logger));
                } else {
                    notification
                            .setText(getString(R.string.notification_success_settings))
                            .setIntent(new Intent(this, SettingsActivity.class));
                }

                notification
                        .setCancellable(!pref_notify_success_lock)
                        .setEnabled(settings.getBoolean("pref_notify_success", true))
                        .show();

                return;

            case Authenticator.STATUS_NOT_REGISTERED:
                notification
                        .setTitle(getString(R.string.notification_not_registered))
                        .setText(getString(R.string.notification_not_registered_register))
                        .setIcon(pref_colored_icons ?
                                R.drawable.ic_notification_register_colored :
                                R.drawable.ic_notification_register)
                        .setIntent(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://wi-fi.ru")))
                        .setEnabled(settings.getBoolean("pref_notify_fail", true))
                        .setId(2)
                        .show();

                notification.setId(0); // Reset ID to default

                return;

            case Authenticator.STATUS_ERROR:
                notification
                        .setTitle(getString(R.string.notification_error))
                        .setText(getString(R.string.notification_error_log))
                        .setIcon(pref_colored_icons ?
                                R.drawable.ic_notification_error_colored :
                                R.drawable.ic_notification_error)
                        .setIntent(new Intent(this, DebugActivity.class).putExtra("logger", logger))
                        .setEnabled(settings.getBoolean("pref_notify_fail", true))
                        .show();
        }
    }

    private boolean isWifiConnected() {
        if (from_shortcut) return true;
        
        // Check if Wi-Fi is not enabled
        if (!manager.isWifiEnabled())
            return false;

        WifiInfo info = manager.getConnectionInfo();

        // Strict check by supplicant state
        if (settings.getBoolean("pref_autoconnect_strict", false))
            if (!info.getSupplicantState().equals(SupplicantState.COMPLETED))
                return false;

        if (connection == null) {
            for (Class<? extends Authenticator> authenticator : Authenticator.SUPPORTED_NETWORKS) {
                try {
                    if (info.getSSID().replace("\"", "").equals(authenticator.getField("SSID").get(authenticator)))
                        return true;
                } catch (Exception ignored) {}
            }
        } else {
            if (connection.getSSID().equals(info.getSSID().replace("\"", "")))
                return true;
        }

        return false;
    }

    private boolean waitForIP() {
        if (from_shortcut) return true;

        int count = 0;

        logger.log_debug(getString(R.string.ip_wait));
        notify_progress
                .setText(getString(R.string.ip_wait))
                .setContinuous()
                .show();

        while (manager.getConnectionInfo().getIpAddress() == 0 && running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}

            if (!isWifiConnected()) {
                logger.log_debug(String.format(
                        getString(R.string.error),
                        getString(R.string.auth_error_network_disconnected)
                ));
                return false;
            }

            if (pref_ip_wait != 0 && count++ == pref_ip_wait) {
                logger.log_debug(String.format(
                        getString(R.string.error),
                        String.format(
                                getString(R.string.ip_wait_result),
                                getString(R.string.not),
                                pref_ip_wait
                        )
                ));
                return false;
            }
        }

        logger.log_debug(String.format(
                getString(R.string.ip_wait_result),
                "", count/2
        ));
        return true;
    }

    private int connect() {
        int result, count = 0;

        do {
            if (!waitForIP()) return Authenticator.STATUS_ERROR;

            if (count > 0) {
                notify_progress
                        .setText(String.format("%s (%s)",
                                getString(R.string.notification_progress_waiting),
                                String.format(
                                        getString(R.string.try_out_of),
                                        count + 1,
                                        pref_retry_count
                                )
                        ))
                        .setContinuous()
                        .show();

                try {
                    Thread.sleep(pref_retry_delay * 1000);
                } catch (InterruptedException ignored) {}
            }

            notify_progress
                    .setText(String.format("%s (%s)",
                            getString(R.string.notification_progress_connecting),
                            String.format(
                                    getString(R.string.try_out_of),
                                    count + 1,
                                    pref_retry_count
                            )
                    ))
                    .show();

            result = connection.start();

            if (!isWifiConnected()) {
                logger.log_debug(String.format(
                        getString(R.string.error),
                        getString(R.string.auth_error_network_disconnected)
                ));
                result = Authenticator.STATUS_ERROR; break;
            }

            if (result == Authenticator.STATUS_NOT_REGISTERED) break;
        } while (++count < pref_retry_count && result > Authenticator.STATUS_ALREADY_CONNECTED && running);

        // Remove progress notification
        notify_progress.hide();

        return result;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if ("STOP".equals(intent.getAction())) { // Stop by intent
            stopSelf();
        } else if (!running) { // Start if not already running
            onStart(intent, startId);
        }
        return START_NOT_STICKY;
    }

    public void onHandleIntent(Intent intent) {
        running = true;
        main(intent);
        running = false;
    }
    
    public void main(Intent intent) {
        // Check if started from one of the shortcuts
        if (intent.getStringExtra("SSID") != null) {
            pref_notify_success_lock = false;
            from_shortcut = true;
        }

        logger.date();

        // Check if Wi-Fi is connected
        if (!isWifiConnected()) return;

        if (from_shortcut) {
            connection = new Chooser(this, true, logger).choose(intent.getStringExtra("SSID"));
        } else {
            connection = new Chooser(this, true, logger).choose();
        }
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

        notify_progress.setTitle(String.format(
                getString(R.string.auth_connecting),
                connection.getSSID()
        ));

        // Try to connect
        int result = connect();

        logger.date();

        // Notify user if still connected to Wi-Fi
        if (isWifiConnected()) notify(result);

        if (from_shortcut || result > Authenticator.STATUS_ALREADY_CONNECTED) return;

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
                    if (network.SSID.replace("\"", "").equals(connection.getSSID())) {
                        manager.enableNetwork(network.networkId, true);
                        manager.reconnect();
                    }
                }
            } catch (NullPointerException ignored) {}
        }
	}
	
	@Override
    public void onDestroy() {
        if (connection != null) connection.stop();
        if (!from_shortcut) notification.hide();
        notify_progress.hide();
    }
}
