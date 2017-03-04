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
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import java.util.concurrent.locks.ReentrantLock;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.DebugActivity;
import pw.thedrhax.mosmetro.activities.SafeViewActivity;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.util.Listener;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Notify;
import pw.thedrhax.util.Util;
import pw.thedrhax.util.WifiUtils;

public class ConnectionService extends IntentService {
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Listener<Boolean> running = new Listener<>(false);
    private static String SSID = WifiUtils.UNKNOWN_SSID;
    private boolean from_shortcut = false;

    // Preferences
    private WifiUtils wifi;
    private SharedPreferences settings;
    private int pref_retry_count;
    private int pref_retry_delay;
    private int pref_ip_wait;
    private boolean pref_colored_icons;

    // Notifications
    private Notify notify;

    // Authenticator
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
        pref_colored_icons = (Build.VERSION.SDK_INT <= 20) ^ settings.getBoolean("pref_notify_alternative", false);

        final PendingIntent stop_intent = PendingIntent.getService(
                this, 0,
                new Intent(this, ConnectionService.class).setAction("STOP"),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        notify = new Notify(this) {
            @Override
            public Notify locked(boolean locked) {
                // Show STOP action only if notification is locked
                if (locked) {
                    if (notify.mActions.size() == 0)
                        notify.addAction(getString(R.string.stop), stop_intent);
                } else {
                    while (notify.mActions.size() > 0) notify.mActions.remove(0);
                }
                return super.locked(locked);
            }
        };

        notify.id(1)
                .onClick(PendingIntent.getActivity(this, 1,
                        new Intent(this, DebugActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT
                ))
                .onDelete(stop_intent)
                .locked(settings.getBoolean("pref_notify_foreground", true));
    }

    private void notify (Provider.RESULT result) {
        notify.hideProgress();

        switch (result) {
            case CONNECTED:
            case ALREADY_CONNECTED:
                if (settings.getBoolean("pref_notify_success_lock", true)) {
                    notify.locked(true);
                }

                notify.title(getString(R.string.notification_success))
                        .text(getString(R.string.notification_success_log))
                        .icon(pref_colored_icons ?
                                R.drawable.ic_notification_success_colored :
                                R.drawable.ic_notification_success)
                        .show()
                        .locked(settings.getBoolean("pref_notify_foreground", true));
                return;

            case NOT_REGISTERED:
                if (settings.getBoolean("pref_notify_fail", true)) {
                    notify.title(getString(R.string.notification_not_registered))
                            .text(getString(R.string.notification_not_registered_register))
                            .icon(pref_colored_icons ?
                                    R.drawable.ic_notification_register_colored :
                                    R.drawable.ic_notification_register)
                            .onClick(PendingIntent.getActivity(this, 0,
                                    new Intent(this, SafeViewActivity.class)
                                            .putExtra("data", "http://wi-fi.ru"),
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            ))
                            .id(2).show().id(1);
                } else {
                    notify.hide();
                }
                return;

            case ERROR:
                if (settings.getBoolean("pref_notify_fail", true)) {
                    notify.title(getString(R.string.notification_error))
                            .text(getString(R.string.notification_error_log))
                            .icon(pref_colored_icons ?
                                    R.drawable.ic_notification_error_colored :
                                    R.drawable.ic_notification_error)
                            .show();
                } else {
                    notify.hide();
                }
                return;

            case NOT_SUPPORTED:
                if (settings.getBoolean("pref_notify_fail", true)) {
                    notify.title(getString(R.string.notification_unsupported))
                            .text(getString(R.string.notification_error_log))
                            .icon(pref_colored_icons ?
                                    R.drawable.ic_notification_register_colored :
                                    R.drawable.ic_notification_register)
                            .show();
                } else {
                    notify.hide();
                }
        }
    }

    private boolean waitForIP() {
        if (from_shortcut) return true;

        int count = 0;

        Logger.log(getString(R.string.ip_wait));
        notify.title(getString(R.string.ip_wait))
                .progress(0, true)
                .show();

        while (wifi.getIP() == 0 && running.get()) {
            SystemClock.sleep(1000);

            if (pref_ip_wait != 0 && count++ == pref_ip_wait) {
                Logger.log(getString(R.string.error,
                        getString(R.string.ip_wait_result,
                            " " + getString(R.string.not), pref_ip_wait
                        )
                ));
                return false;
            }
        }

        Logger.log(getString(R.string.ip_wait_result, "", count/2));
        return true;
    }

    private Provider.RESULT connect() {
        Provider.RESULT result;
        int count = 0;

        do {
            if (count > 0) {
                notify.text(String.format("%s (%s)",
                                getString(R.string.notification_progress_waiting),
                                getString(R.string.try_out_of, count + 1, pref_retry_count)
                        ))
                        .progress(0, true)
                        .show();

                SystemClock.sleep(pref_retry_delay * 1000);
            }

            notify.text(String.format("%s (%s)",
                            getString(R.string.notification_progress_connecting),
                            getString(R.string.try_out_of, count + 1, pref_retry_count)
                    ))
                    .show();

            result = provider.start();

            if (result == Provider.RESULT.NOT_REGISTERED) break;
            if (from_shortcut) break;
        } while (++count < pref_retry_count && running.get() && result == Provider.RESULT.ERROR);

        return result;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        if ("STOP".equals(intent.getAction())) { // Stop by intent
            running.set(false);
            return START_NOT_STICKY;
        }

        if (intent.getBooleanExtra("debug", false)) {
            from_shortcut = true;
            notify.enabled(false);
        } else {
            from_shortcut = intent.getBooleanExtra("force", false);
        }
        SSID = wifi.getSSID(intent);

        if (!running.get()) // Ignore if service is already running
            if (!WifiUtils.UNKNOWN_SSID.equals(SSID) || from_shortcut)
                if (Provider.isSSIDSupported(SSID) || from_shortcut) {
                    onStart(intent, startId);
                } else {
                    running.set(false);
                }

        return START_NOT_STICKY;
    }

    public void onHandleIntent(Intent intent) {
        if (lock.tryLock()) {
            running.set(true);
            main();
            running.set(false);
            lock.unlock();

            notify.hide();
            if (from_shortcut) {
                // TODO: Do not start connection in DebugActivity after click on this notification
                notify.cancelOnClick(true).locked(false).show();
            }
            sendBroadcast(new Intent("pw.thedrhax.mosmetro.event.ConnectionService")
                    .putExtra("RUNNING", false)
            );
        }
    }

    private void main() {
        sendBroadcast(new Intent("pw.thedrhax.mosmetro.event.ConnectionService")
                .putExtra("RUNNING", true)
        );

        Logger.date();
        notify.icon(pref_colored_icons ?
                R.drawable.ic_notification_connecting_colored :
                R.drawable.ic_notification_connecting);

        // Wait for IP before detecting the Provider
        if (!waitForIP()) {
            notify(Provider.RESULT.ERROR);
            return;
        }

        notify.title(getString(R.string.auth_connecting, SSID))
                .text(getString(R.string.auth_provider_check))
                .progress(0, true)
                .show();

        provider = Provider.find(this, running)
                .setRunningListener(running)
                .setCallback(new Provider.ICallback() {
                    @Override
                    public void onProgressUpdate(int progress) {
                        notify.progress(progress).show();
                    }
                });

        notify.text(getString(R.string.auth_waiting)).show();

        // Try to connect
        Provider.RESULT result = connect();

        // Notify user if not interrupted
        if (running.get()) {
            Logger.date();
            notify(result);
        } else {
            notify.hide();
            return;
        }

        // Stop the service if connection were unsuccessful or started from shortcut
        switch (result) {
            case CONNECTED:
            case ALREADY_CONNECTED:
                if (!from_shortcut) break;
            default:
                return;
        }

        sendBroadcast(new Intent("pw.thedrhax.mosmetro.event.CONNECTED")
                .putExtra("SSID", SSID)
                .putExtra("PROVIDER", provider.getName())
        );

        // Wait while internet connection is available
        int count = 0;
        while (running.get()) {
            SystemClock.sleep(1000);

            // Check internet connection each 10 seconds
            if (settings.getBoolean("pref_internet_check", true) && ++count == 10) {
                count = 0;
                if (!provider.isConnected())
                    break;
            }
        }

        sendBroadcast(new Intent("pw.thedrhax.mosmetro.event.DISCONNECTED"));
        notify.hide();

        // Try to reconnect the Wi-Fi network
        if (settings.getBoolean("pref_wifi_reconnect", false)) wifi.reconnect(SSID);

        // If Service is not being killed, continue the main loop
        if (running.get()) main();
	}

    public static boolean isRunning() {
        return running.get();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (!settings.getBoolean("pref_notify_foreground", true)) {
            running.set(false);
        }
    }
}
