package pw.thedrhax.mosmetro.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.DebugActivity;
import pw.thedrhax.mosmetro.activities.SettingsActivity;
import pw.thedrhax.mosmetro.authenticator.Authenticator;
import pw.thedrhax.mosmetro.authenticator.Chooser;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Notification;
import pw.thedrhax.util.WifiUtils;

/*
 * ConnectionService lifecycle
 *
 * onStartCommand()
 * |   | → WaitForIPTask → | → AuthService → | → NetMonitorTask → |
 * |       ↑               | → stopSelf()    |                    |
 * |       ↑ ----------on fail------------ ← |                    |
 * |       ↑ ------------------on disconnect------------------- ← |
 * |
 * | → WiFiMonitorTask → stopSelf()
 * | → stopSelf() (ACTION_STOP)
 */

public class ConnectionService extends Service {
    public static final String ACTION_STOP = "STOP";
    public static final String ACTION_SHORTCUT = "SHORTCUT";

    private Logger logger;
    private AsyncTask conn_task;
    private WiFiMonitorTask wifi_task;
    private Authenticator connection;

    private int count = 0;
    private int pref_retry_count;
    private boolean running = false;
    private boolean from_shortcut = false;

    // Android-specific
    private WifiManager manager;
    private SharedPreferences settings;
    private WifiUtils wifi;

    private boolean pref_colored_icons;

    private Notification notify_progress;
    private Notification notification;

    @Override
    public void onCreate() {
        super.onCreate();

        logger = new Logger();
        manager = (WifiManager) getSystemService(WIFI_SERVICE);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        wifi = new WifiUtils(this);

        pref_colored_icons = (Build.VERSION.SDK_INT <= 20)
                              || settings.getBoolean("pref_notify_alternative", false);
        pref_retry_count = Integer.parseInt(settings.getString("pref_retry_count", "3"));

        PendingIntent delete_intent = PendingIntent.getService(
                this, 0,
                new Intent(this, ConnectionService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        notification = new Notification(this)
                .setId(0)
                .setDeleteIntent(delete_intent);

        notify_progress = new Notification(this)
                .setIcon(pref_colored_icons ?
                        R.drawable.ic_notification_connecting_colored :
                        R.drawable.ic_notification_connecting)
                .setId(1)
                .setEnabled(settings.getBoolean("pref_notify_progress", true)
                        && (Build.VERSION.SDK_INT >= 14))
                .setDeleteIntent(delete_intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null) return START_NOT_STICKY;
		
        // Stop from notification
        if (ACTION_STOP.equals(intent.getAction())) {
            stopSelf(); return START_NOT_STICKY;
        }

        // Check if already running
        if (running) {
            return START_NOT_STICKY;
        } else {
            running = true;
        }

        conn_task = new WaitForIPTask();

        // Create new Authenticator instance
        if (ACTION_SHORTCUT.equals(intent.getAction())) {
            connection = new Chooser(this, false, logger)
                    .choose(intent.getStringExtra(DebugActivity.EXTRA_SSID));
            from_shortcut = true;
        }

        if (connection == null)
            connection = new Chooser(this, true, logger).choose(intent);

        if (connection == null) {
            stopSelf(); return START_NOT_STICKY;
        }

        // Start connection sequence
        ((WaitForIPTask) conn_task).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);

        // Start Wi-Fi monitoring
        wifi_task = new WiFiMonitorTask();
        wifi_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (conn_task != null) conn_task.cancel(false);
        if (wifi_task != null) wifi_task.cancel(false);
        if (auth_binder != null) auth_binder.stop();

        notify_progress.setEnabled(false).hide();
        notification.setEnabled(false).hide();

        running = false;

        super.onDestroy();
    }

    private void onFinish (int result) {
        notify_progress.hide();

        switch (result) {
            case Authenticator.STATUS_CONNECTED:
            case Authenticator.STATUS_ALREADY_CONNECTED:
                notification
                        .setTitle(getString(R.string.notification_success))
                        .setIcon(pref_colored_icons ?
                                R.drawable.ic_notification_success_colored :
                                R.drawable.ic_notification_success)
                        .setCancellable(!settings.getBoolean("pref_notify_success_lock", true) || from_shortcut)
                        .setEnabled(settings.getBoolean("pref_notify_success", true));

                if (settings.getBoolean("pref_notify_success_log", false)) {
                    Intent debug = new Intent(this, DebugActivity.class);
                    debug.setAction(DebugActivity.ACTION_SHOW_LOG);
                    debug.putExtra(DebugActivity.EXTRA_LOGGER, logger);

                    notification
                            .setText(getString(R.string.notification_success_log))
                            .setIntent(debug);
                } else {
                    notification
                            .setText(getString(R.string.notification_success_settings))
                            .setIntent(new Intent(this, SettingsActivity.class));
                }

                notification.show();

                notification.setCancellable(true);

                return;

            case Authenticator.STATUS_NOT_REGISTERED:
                Intent registration = new Intent(Intent.ACTION_VIEW);
                registration.setData(Uri.parse("http://wi-fi.ru"));

                notification
                        .setTitle(getString(R.string.notification_not_registered))
                        .setText(getString(R.string.notification_not_registered_register))
                        .setIcon(pref_colored_icons ?
                                R.drawable.ic_notification_register_colored :
                                R.drawable.ic_notification_register)
                        .setIntent(registration)
                        .setEnabled(settings.getBoolean("pref_notify_fail", true))
                        .setId(2)
                        .show();

                notification.setId(0); // Reset ID to default

                return;

            case Authenticator.STATUS_ERROR:
                Intent debug = new Intent(this, DebugActivity.class);
                debug.setAction(DebugActivity.ACTION_SHOW_LOG);
                debug.putExtra(DebugActivity.EXTRA_LOGGER, logger);

                notification
                        .setId(2)
                        .setTitle(getString(R.string.notification_error))
                        .setText(getString(R.string.notification_error_log))
                        .setIcon(pref_colored_icons ?
                                R.drawable.ic_notification_error_colored :
                                R.drawable.ic_notification_error)
                        .setIntent(debug)
                        .setEnabled(settings.getBoolean("pref_notify_fail", true))
                        .show();

                notification.setId(0);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*
     * Async IP check
     */

    private class WaitForIPTask extends AsyncTask<Void,Object,Boolean> {
        private Logger local_logger;
        private int count = 0;

        private int pref_ip_wait;

        public WaitForIPTask() {
            pref_ip_wait = Integer.parseInt(settings.getString("pref_ip_wait", "30"));

            local_logger = new Logger() {
                @Override
                public void log(LEVEL level, String message) {
                    super.log(level, message);
                    publishProgress(level, message);
                }
            };
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            int count = 0;

            if (from_shortcut) return true;

            while (manager.getConnectionInfo().getIpAddress() == 0 && !isCancelled()) {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

                if (pref_ip_wait != 0) {
                    notify_progress
                            .setProgress(count * 100 / pref_ip_wait)
                            .show();

                    if (count++ == pref_ip_wait) { // Timeout condition
                        local_logger.log(String.format(
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
            }

            return !isCancelled();
        }

        @Override
        protected void onPreExecute() {
            logger.log(getString(R.string.ip_wait));
            notify_progress
                    .setTitle(String.format(
                            getString(R.string.auth_connecting),
                            connection.getSSID()
                    ))
                    .setText(getString(R.string.ip_wait))
                    .setContinuous()
                    .show();
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            logger.log((Logger.LEVEL) values[0], (String) values[1]);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                logger.log(String.format(
                        getString(R.string.ip_wait_result),
                        "", count/2
                ));
                notify_progress.setContinuous(); // Return to defaults

                bindService(
                        new Intent(ConnectionService.this, AuthService.class),
                        auth_conn, BIND_AUTO_CREATE
                );
            } else {
                onFinish(Authenticator.STATUS_ERROR);
                stopSelf();
            }
        }
    }

    /*
     * AuthService bindings
     */

    AuthService.AuthBinder auth_binder = null;
    ServiceConnection auth_conn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            auth_binder = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            auth_binder = (AuthService.AuthBinder) service;
            auth_binder.setLogger(logger);
            auth_binder.setCallback(new AuthService.Callback() {
                @Override
                public void onPreExecute() {
                    notify_progress
                            .setText(String.format("%s (%s)",
                                    getString(R.string.notification_progress_connecting),
                                    String.format(
                                            getString(R.string.try_out_of),
                                            count + 1, pref_retry_count
                                    )
                            ))
                            .show();
                }

                @Override
                public void onPostExecute(int result) {
                    if (result < Authenticator.STATUS_ERROR) {
                        onFinish(result);
                        conn_task = new NetMonitorTask();
                        ((NetMonitorTask) conn_task).executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR,
                                (Void[]) null
                        );
                    } else {
                        if (++count < pref_retry_count) {
                            conn_task = new WaitForIPTask();
                            ((WaitForIPTask) conn_task).executeOnExecutor(
                                    AsyncTask.THREAD_POOL_EXECUTOR,
                                    (Void[]) null
                            );
                        } else {
                            onFinish(result);
                        }
                    }

                    try {
                        ConnectionService.this.unbindService(auth_conn);
                    } catch (IllegalArgumentException ignored) {}
                }

                @Override
                public void onCancelled() {
                    try {
                        ConnectionService.this.unbindService(auth_conn);
                    } catch (IllegalArgumentException ignored) {}
                }
            });

            connection.setProgressListener(new Authenticator.ProgressListener() {
                @Override
                public void onProgressUpdate(int progress) {
                    notify_progress
                            .setProgress(progress)
                            .show();
                }
            });

            auth_binder.start(connection);
        }
    };

    /*
     * Wi-Fi monitoring
     */

    private class WiFiMonitorTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... params) {
            while (connection.getSSID().equals(wifi.get()) && !isCancelled()) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
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

            ConnectionService.this.stopSelf();
        }
    }

    /*
     * Network monitoring task
     */

    private class NetMonitorTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... params) {
            int count = 0;

            // Wait while internet connection is available
            while (!isCancelled()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}

                // Check internet connection each 10 seconds
                if (++count == 10) {
                    count = 0;
                    if (connection.isConnected() != Authenticator.CHECK_CONNECTED)
                        break;
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            conn_task = new WaitForIPTask();
            ((WaitForIPTask) conn_task).executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR,
                    (Void[]) null
            );
        }
    }
}
