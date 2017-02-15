/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 * Copyright © 2015 Dmitry Karikh <the.dr.hax@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pw.thedrhax.mosmetro.services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.DebugActivity;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Notification;
import pw.thedrhax.util.Util;
import pw.thedrhax.util.WifiUtils;

public class ConnectionService extends IntentService {
    private static boolean running = false;
    private static String SSID = WifiUtils.UNKNOWN_SSID;
    private boolean from_shortcut = false;

    // Preferences
    private WifiUtils wifi;
    private SharedPreferences settings;
    private int pref_retry_count;
    private int pref_retry_delay;
    private int pref_ip_wait;
    private boolean pref_notify_success_lock;

    // Notifications
    private Notification notify_progress;
    private Notification notification;

    // Authenticator
    private Logger logger;
    private Provider provider;

    public ConnectionService () {
		super("ConnectionService");
	}

    @Override
    public void onCreate() {
		super.onCreate();

        wifi = new WifiUtils(this) {
            @Override
            public boolean isConnected(String SSID) {
                return from_shortcut || super.isConnected(SSID);
            }
        };
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        pref_retry_count = Util.getIntPreference(this, "pref_retry_count", 3);
        pref_retry_delay = Util.getIntPreference(this, "pref_retry_delay", 5);
        pref_ip_wait = Util.getIntPreference(this, "pref_ip_wait", 30);
        pref_notify_success_lock = settings.getBoolean("pref_notify_success_lock", true);

        PendingIntent delete_intent = PendingIntent.getService(
                this, 0,
                new Intent(this, ConnectionService.class).setAction("STOP"),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        notify_progress = new Notification(this)
                .setIcon(R.drawable.ic_notification_connecting)
                .setId(1)
                .setEnabled(settings.getBoolean("pref_notify_progress", true)
                            && Build.VERSION.SDK_INT >= 14)
                .setIntent(new Intent(this, DebugActivity.class))
                .setDeleteIntent(delete_intent);

        notification = new Notification(this)
                .setId(0)
                .setIntent(new Intent(this, DebugActivity.class))
                .setDeleteIntent(delete_intent);

        logger = Logger.getLogger();
    }

    private void notify (Provider.RESULT result) {
        switch (result) {
            case CONNECTED:
            case ALREADY_CONNECTED:
                if (settings.getBoolean("pref_notify_success", true)) {
                    notification
                            .setTitle(getString(R.string.notification_success))
                            .setIcon(R.drawable.ic_notification_success)
                            .setText(getString(R.string.notification_success_log))
                            .setCancellable(from_shortcut || !pref_notify_success_lock)
                            .show();

                    notification.setCancellable(true);
                }
                return;

            case NOT_REGISTERED:
                if (settings.getBoolean("pref_notify_fail", true)) {
                    notification
                            .setTitle(getString(R.string.notification_not_registered))
                            .setText(getString(R.string.notification_not_registered_register))
                            .setIcon(R.drawable.ic_notification_register)
                            .setIntent(new Intent(Intent.ACTION_VIEW)
                                    .setData(Uri.parse("http://wi-fi.ru")))
                            .setId(2)
                            .show();

                    notification.setId(0); // Reset ID to default
                }
                return;

            case ERROR:
                if (settings.getBoolean("pref_notify_fail", true)) {
                    notification
                            .setTitle(getString(R.string.notification_error))
                            .setText(getString(R.string.notification_error_log))
                            .setIcon(R.drawable.ic_notification_error)
                            .show();
                }
                return;

            case NOT_SUPPORTED:
                if (settings.getBoolean("pref_notify_fail", true)) {
                    notification
                            .setTitle(getString(R.string.notification_unsupported))
                            .setText(getString(R.string.notification_error_log))
                            .setIcon(R.drawable.ic_notification_register)
                            .show();
                }
        }
    }

    private boolean waitForIP() {
        if (from_shortcut) return true;

        int count = 0;

        logger.log(getString(R.string.ip_wait));
        notify_progress
                .setText(getString(R.string.ip_wait))
                .setContinuous()
                .show();

        while (wifi.getIP() == 0 && running) {
            SystemClock.sleep(1000);

            if (pref_ip_wait != 0 && count++ == pref_ip_wait) {
                logger.log(getString(R.string.error,
                        getString(R.string.ip_wait_result,
                            " " + getString(R.string.not), pref_ip_wait
                        )
                ));
                return false;
            }
        }

        logger.log(getString(R.string.ip_wait_result, "", count/2));
        return true;
    }

    private Provider.RESULT connect() {
        Provider.RESULT result;
        int count = 0;

        do {
            if (count > 0) {
                notify_progress
                        .setText(String.format("%s (%s)",
                                getString(R.string.notification_progress_waiting),
                                getString(R.string.try_out_of, count + 1, pref_retry_count)
                        ))
                        .setContinuous()
                        .show();

                SystemClock.sleep(pref_retry_delay * 1000);
            }

            notify_progress
                    .setText(String.format("%s (%s)",
                            getString(R.string.notification_progress_connecting),
                            getString(R.string.try_out_of, count + 1, pref_retry_count)
                    ))
                    .show();

            result = provider.start();

            if (result == Provider.RESULT.NOT_REGISTERED) break;
        } while (++count < pref_retry_count && running && result == Provider.RESULT.ERROR);

        return result;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if ("STOP".equals(intent.getAction())) { // Stop by intent
            running = false;
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent.getBooleanExtra("debug", false)) {
            from_shortcut = true;
            notify_progress.setEnabled(false);
            notification.setEnabled(false);
        } else {
            from_shortcut = intent.getBooleanExtra("force", false);
        }
        SSID = wifi.getSSID(intent);

        if (!running) // Ignore if service is already running
            if (!WifiUtils.UNKNOWN_SSID.equals(SSID) || from_shortcut)
                if (Provider.isSSIDSupported(SSID) || from_shortcut) {
                    onStart(intent, startId);
                } else {
                    running = false;
                    stopSelf();
                }

        return START_NOT_STICKY;
    }

    public void onHandleIntent(Intent intent) {
        sendBroadcast(new Intent("pw.thedrhax.mosmetro.event.ConnectionService")
                .putExtra("RUNNING", true)
        );
        running = true;

        logger.date();
        notification.hide();

        // Wait for IP before detecting the Provider
        if (!waitForIP()) {
            notify_progress.hide();
            notify(Provider.RESULT.ERROR);
            running = false; return;
        }

        notify_progress
                .setTitle(getString(R.string.auth_connecting, SSID))
                .setText(getString(R.string.auth_provider_check))
                .setContinuous()
                .show();

        provider = Provider.find(this, logger)
                .setCallback(new Provider.ICallback() {
                    @Override
                    public void onProgressUpdate(int progress) {
                        notify_progress
                                .setProgress(progress)
                                .show();
                    }
                });

        notify_progress
                .setText(getString(R.string.auth_waiting))
                .show();

        // Try to connect
        Provider.RESULT result = connect();
        notify_progress.hide();

        // Notify user if not interrupted
        if (running) {
            logger.date();
            notify(result);
        } else {
            return;
        }

        // Stop the service if connection were unsuccessful or started from shortcut
        switch (result) {
            case CONNECTED:
            case ALREADY_CONNECTED:
                if (!from_shortcut) break;
            default:
                running = false; return;
        }

        sendBroadcast(new Intent("pw.thedrhax.mosmetro.event.CONNECTED")
                .putExtra("SSID", SSID)
                .putExtra("PROVIDER", provider.getName())
        );

        // Wait while internet connection is available
        int count = 0;
        while (running) {
            SystemClock.sleep(1000);

            // Check internet connection each 10 seconds
            if (settings.getBoolean("pref_internet_check", true) && ++count == 10) {
                count = 0;
                if (!provider.isConnected())
                    break;
            }
        }

        sendBroadcast(new Intent("pw.thedrhax.mosmetro.event.DISCONNECTED"));
        notification.hide();

        // Try to reconnect the Wi-Fi network
        if (settings.getBoolean("pref_wifi_reconnect", false)) wifi.reconnect(SSID);

        // If Service is not being killed, continue the main loop
        if (running) onHandleIntent(intent);
	}

    public static boolean isRunning() {
        return running;
    }

    @Override
    public void onDestroy() {
    	running = false;
        SSID = WifiUtils.UNKNOWN_SSID;
        if (provider != null) provider.stop();
        if (!from_shortcut) notification.hide();
        notify_progress.hide();
        sendBroadcast(new Intent("pw.thedrhax.mosmetro.event.ConnectionService")
                .putExtra("RUNNING", false)
        );
    }
}
