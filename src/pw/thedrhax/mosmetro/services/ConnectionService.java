package pw.thedrhax.mosmetro.services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
    private static final String UNKNOWN_SSID = "<unknown ssid>";

    private static boolean running = false;
    private static String SSID = UNKNOWN_SSID;
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
        pref_notify_success_lock = settings.getBoolean("pref_notify_success_lock", true);

        PendingIntent delete_intent = PendingIntent.getService(
                this, 0,
                new Intent(this, ConnectionService.class).setAction("STOP"),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        notify_progress = new Notification(this)
                .setIcon(pref_colored_icons ?
                        R.drawable.ic_notification_connecting_colored :
                        R.drawable.ic_notification_connecting)
                .setId(1)
                .setEnabled(settings.getBoolean("pref_notify_progress", true) && (Build.VERSION.SDK_INT >= 14))
                .setDeleteIntent(delete_intent);

        notification = new Notification(this)
                .setId(0)
                .setDeleteIntent(delete_intent);

        logger = new Logger();
    }

    private void notify (Authenticator.RESULT result) {
        if (!running) return;

        switch (result) {
            case CONNECTED:
            case ALREADY_CONNECTED:
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

                notification.setCancellable(true);

                return;

            case NOT_REGISTERED:
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

            case ERROR:
                notification
                        .setTitle(getString(R.string.notification_error))
                        .setText(getString(R.string.notification_error_log))
                        .setIcon(pref_colored_icons ?
                                R.drawable.ic_notification_error_colored :
                                R.drawable.ic_notification_error)
                        .setIntent(new Intent(this, DebugActivity.class).putExtra("logger", logger))
                        .setEnabled(settings.getBoolean("pref_notify_fail", true))
                        .show();

                return;

            case UNSUPPORTED:
                notification
                        .setTitle(getString(R.string.notification_unsupported))
                        .setText(getString(R.string.notification_error_log))
                        .setIcon(pref_colored_icons ?
                                R.drawable.ic_notification_register_colored :
                                R.drawable.ic_notification_register)
                        .setIntent(new Intent(this, DebugActivity.class).putExtra("logger", logger))
                        .setEnabled(settings.getBoolean("pref_notify_fail", true))
                        .show();
        }
    }

    private boolean isWifiConnected() {
        if (from_shortcut) return true;
        
        return manager.isWifiEnabled() && connection.getSSID().equals(SSID);
    }

    private boolean waitForIP() {
        if (from_shortcut) return true;

        int count = 0;

        logger.log(getString(R.string.ip_wait));
        notify_progress
                .setText(getString(R.string.ip_wait))
                .setContinuous()
                .show();

        while (manager.getConnectionInfo().getIpAddress() == 0 && running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}

            if (!isWifiConnected()) {
                logger.log(String.format(
                        getString(R.string.error),
                        getString(R.string.auth_error_network_disconnected)
                ));
                return false;
            }

            if (pref_ip_wait != 0 && count++ == pref_ip_wait) {
                logger.log(String.format(
                        getString(R.string.error),
                        String.format(
                                getString(R.string.ip_wait_result),
                                " " + getString(R.string.not),
                                pref_ip_wait
                        )
                ));
                return false;
            }
        }

        logger.log(String.format(
                getString(R.string.ip_wait_result),
                "", count/2
        ));
        return true;
    }

    private Authenticator.RESULT connect() {
        Authenticator.RESULT result;
        int count = 0;

        do {
            if (!waitForIP()) return Authenticator.RESULT.ERROR;

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
                logger.log(String.format(
                        getString(R.string.error),
                        getString(R.string.auth_error_network_disconnected)
                ));
                result = Authenticator.RESULT.ERROR; break;
            }

            if (result == Authenticator.RESULT.NOT_REGISTERED) break;
        } while (++count < pref_retry_count && running && result == Authenticator.RESULT.ERROR);

        return result;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getBooleanExtra("background", false)) {
            SSID = intent.getStringExtra("SSID");
            pref_notify_success_lock = false;
            from_shortcut = true;
        }

        if (!from_shortcut || SSID.isEmpty()) {
            WifiInfo info;
            if (Build.VERSION.SDK_INT >= 14) {
                info = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            } else {
                info = manager.getConnectionInfo();
            }
            SSID = (info != null && info.getSSID() != null)
                    ? info.getSSID().replace("\"", "") : UNKNOWN_SSID;
        }

        if ("STOP".equals(intent.getAction())) { // Stop by intent
            stopSelf();
        } else if (!(UNKNOWN_SSID.equals(SSID) || running)) {
            onStart(intent, startId);
        }
        return START_NOT_STICKY;
    }

    public void onHandleIntent(Intent intent) {
        running = true;
        main();
        running = false;
    }
    
    private void main() {
        logger.date();

        // Select an Authenticator
        connection = new Chooser(this, logger).choose(SSID);
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

        notify_progress
            .setTitle(String.format(
                getString(R.string.auth_connecting),
                connection.getSSID()
            ))
            .setText(getString(R.string.auth_waiting))
            .setContinuous()
            .show();

        try {
            if (!from_shortcut)
                Thread.sleep(5000);
        } catch (InterruptedException ignored) {}

        // Try to connect
        Authenticator.RESULT result = connect();
        notify_progress.hide();

        logger.date();

        // Notify user if still connected to Wi-Fi
        if (isWifiConnected()) notify(result);

        if (from_shortcut || !(result == Authenticator.RESULT.ALREADY_CONNECTED
                || result == Authenticator.RESULT.CONNECTED)) return;

        // Wait while internet connection is available
        int count = 0;
        while (isWifiConnected()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}

            // Check internet connection each 10 seconds
            if (settings.getBoolean("pref_internet_check", true) && ++count == 10) {
                count = 0;
                if (connection.isConnected() != Authenticator.CHECK.CONNECTED)
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
