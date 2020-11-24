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
import android.preference.PreferenceManager;

import java.util.concurrent.locks.ReentrantLock;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.DebugActivity;
import pw.thedrhax.mosmetro.activities.SafeViewActivity;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.util.Listener;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Notify;
import pw.thedrhax.util.Randomizer;
import pw.thedrhax.util.Util;
import pw.thedrhax.util.Version;
import pw.thedrhax.util.WifiUtils;

public class ConnectionService extends IntentService {
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Listener<Boolean> running = new Listener<>(false);
    private static String SSID = WifiUtils.UNKNOWN_SSID;
    private boolean from_shortcut = false;
    private boolean from_debug = false;

    // Preferences
    private WifiUtils wifi;
    private SharedPreferences settings;
    private int pref_retry_count;
    private int pref_ip_wait;
    private boolean pref_notify_foreground;

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
        pref_ip_wait = Util.getIntPreference(this, "pref_ip_wait", 0);
        pref_notify_foreground = settings.getBoolean("pref_notify_foreground", true);

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

        Intent debug = new Intent(this, DebugActivity.class);
        debug.putExtra(DebugActivity.INTENT_VIEW_ONLY, true);

        notify.id(1)
                .onClick(PendingIntent.getActivity(
                        this, 1, debug,
                        PendingIntent.FLAG_UPDATE_CURRENT
                ))
                .onDelete(stop_intent)
                .locked(pref_notify_foreground);
    }

    public boolean isFromDebug() {
        return from_debug;
    }

    private void notify (Provider.RESULT result) {
        notify.hideProgress();

        switch (result) {
            case CONNECTED:
            case ALREADY_CONNECTED:
                boolean enabled = !pref_notify_foreground;
                enabled &= !settings.getBoolean("pref_notify_success", true);

                if (enabled || from_debug) {
                    notify.hide();
                    break;
                }

                if (settings.getBoolean("pref_notify_success_lock", true)) {
                    notify.locked(true);
                }

                if (from_shortcut) {
                    // TODO: Auto cancel this notification after 30 seconds
                    notify.id(2) // protect this notification from removing
                            .cancelOnClick(true)
                            .locked(false);
                }

                notify.title(getString(R.string.notification_success))
                        .text(getString(R.string.notification_success_log))
                        .icon(R.drawable.ic_notification_success_colored,
                              R.drawable.ic_notification_success)
                        .show();
                break;

            case NOT_REGISTERED:
                notify.hide()
                        .title(getString(R.string.notification_not_registered))
                        .text(getString(R.string.notification_not_registered_register))
                        .icon(R.drawable.ic_notification_register_colored,
                              R.drawable.ic_notification_register)
                        .onClick(PendingIntent.getActivity(this, 0,
                                new Intent(this, SafeViewActivity.class)
                                        .putExtra("data", "http://wi-fi.ru"),
                                PendingIntent.FLAG_UPDATE_CURRENT))
                        .id(2).locked(false).show();
                break;

            case ERROR:
                notify.hide()
                        .title(getString(R.string.notification_error))
                        .text(getString(R.string.notification_error_log))
                        .icon(R.drawable.ic_notification_error_colored,
                              R.drawable.ic_notification_error)
                        .enabled(!from_debug && settings.getBoolean("pref_notify_fail", false))
                        .id(2).locked(false).show();
                break;

            case NOT_SUPPORTED:
                notify.hide()
                        .title(getString(R.string.notification_unsupported))
                        .text(getString(R.string.notification_error_log))
                        .icon(R.drawable.ic_notification_register_colored,
                              R.drawable.ic_notification_register)
                        .enabled(!from_debug && settings.getBoolean("pref_notify_fail", false))
                        .id(2).locked(false).show();
                break;

            case INTERRUPTED: // impossible, but IDE thinks otherwise
                notify.hide();
                break;
        }

        notify // return to defaults
                .id(1)
                .cancelOnClick(false)
                .locked(pref_notify_foreground);
    }

    private boolean waitForIP() {
        if (from_shortcut) return true;

        int count = 0;

        Logger.log(getString(R.string.ip_wait));
        notify.title(getString(R.string.ip_wait))
                .progress(0, true)
                .show();

        while (wifi.getIP() == 0) {
            if (!running.sleep(1000)) return false;

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
        int pref_retry_delay = Util.getIntPreference(this, "pref_retry_delay", 5) * 1000;

        do {
            if (count > 0) {
                String msg = String.format("%s (%s)",
                                getString(R.string.notification_progress_waiting),
                                getString(R.string.try_out_of, count + 1, pref_retry_count)
                );

                Logger.log(msg);
                notify.text(msg).progress(0, true).show();

                if (!running.sleep(pref_retry_delay)) {
                    result = Provider.RESULT.INTERRUPTED;
                    break;
                }
            }

            result = provider.start();

            if (result == Provider.RESULT.NOT_REGISTERED) break;
            if (from_shortcut) break;
        } while (++count < pref_retry_count && running.get() && result == Provider.RESULT.ERROR);

        return result;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        if ("STOP".equals(intent.getAction()) || intent.getBooleanExtra("stop", false)) { // Stop by intent
            Logger.log(this, "Stopping by Intent");
            running.set(false);
            return START_NOT_STICKY;
        }

        if (intent.getBooleanExtra("debug", false)) {
            Logger.log(this, "Started from DebugActivity");
            from_shortcut = true;
            from_debug = true;
        } else if (intent.getBooleanExtra("force", false)) {
            Logger.log(this, "Started from shortcut");
            from_shortcut = true;
            from_debug = false;
        } else {
            Logger.log(this, "Started by system");
            from_shortcut = false;
            from_debug = false;
        }

        SSID = wifi.getSSID(intent);

        if (!running.get() && lock.isLocked()) {
            // Service is shutting down. Trying to interrupt
            running.set(true);
        }

        if (!running.get() && !lock.isLocked()) // Ignore if service is already running
            if (!WifiUtils.UNKNOWN_SSID.equals(SSID) || from_shortcut)
                if (Provider.isSSIDSupported(SSID) || from_shortcut)
                    onStart(intent, startId);

        return START_NOT_STICKY;
    }

    public void onHandleIntent(Intent intent) {
        if (lock.tryLock()) {
            Logger.log(this, "Broadcast | ConnectionService (RUNNING = true)");
            sendBroadcast(new Intent("pw.thedrhax.mosmetro.event.ConnectionService")
                    .putExtra("RUNNING", true)
            );

            Logger.date(">>> ");
            Logger.log(getString(R.string.version, Version.getFormattedVersion()));
            Logger.log(getString(R.string.auth_connecting, SSID));

            running.set(true);
            boolean first_iteration = true;
            while (running.get()) {
                if (!first_iteration) {
                    Logger.log(this, "Still alive!");
                } else {
                    first_iteration = false;
                }

                main();
            }
            lock.unlock();

            notify.hide();

            Logger.log(this, "Broadcast | ConnectionService (RUNNING = false)");
            sendBroadcast(new Intent("pw.thedrhax.mosmetro.event.ConnectionService")
                    .putExtra("RUNNING", false)
            );

            Logger.date("<<< ");
        } else {
            Logger.log(this, "Already running");
        }
    }

    private void main() {
        notify.icon(R.drawable.ic_notification_connecting_colored,
                    R.drawable.ic_notification_connecting);

        // Wait for IP before detecting the Provider
        if (!waitForIP()) {
            if (running.get()) {
                notify(Provider.RESULT.ERROR);
                running.set(false);
            }
            return;
        }

        // Apply random delay
        if (!from_debug && !settings.getBoolean("pref_delay_always", false)) {
            Logger.log(getString(R.string.notification_progress_waiting));
            notify.title(getString(R.string.notification_progress_waiting))
                    .progress(0, true)
                    .show();

            if (!new Randomizer(this).delay(running)) return;
        }

        new Notify(this).id(2).hide(); // hide error notification

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

                    @Override
                    public void onProgressUpdate(int progress, String message) {
                        notify.text(message).progress(progress).show();
                    }
                });

        notify.text(getString(R.string.auth_waiting)).show();

        // Try to connect
        Logger.log(getString(R.string.algorithm_name, provider.getName()));
        Provider.RESULT result = connect();

        // Notify user if not interrupted
        if (running.get()) {
            notify(result);
        } else {
            return;
        }

        // Stop the service if connection were unsuccessful or started from shortcut
        switch (result) {
            case CONNECTED:
            case ALREADY_CONNECTED:
                if (Build.VERSION.SDK_INT >= 21) wifi.report(true);
                if (!from_shortcut) break;
            default:
                Logger.log(this, "Stopping by result (" + result.name() + ")");
                running.set(false);
                return;
        }

        Logger.log(this, "Broadcast | CONNECTED");
        sendBroadcast(new Intent("pw.thedrhax.mosmetro.event.CONNECTED")
                .putExtra("SSID", SSID)
                .putExtra("PROVIDER", provider.getName())
        );

        // Wait while internet connection is available
        int count = 0;
        while (running.sleep(1000)) {
            // Check internet connection each 10 seconds
            int check_interval = Util.getIntPreference(this, "pref_internet_check_interval", 10);
            if (settings.getBoolean("pref_internet_check", true) && ++count == check_interval) {
                Logger.log(this, "Checking internet connection");
                count = 0;
                if (!provider.isConnected(false))
                    break;
            }
        }

        Logger.log(this, "Broadcast | DISCONNECTED");
        sendBroadcast(new Intent("pw.thedrhax.mosmetro.event.DISCONNECTED"));
        notify.hide();

        // Try to reconnect the Wi-Fi network
        if (settings.getBoolean("pref_wifi_reconnect", false)) {
            Logger.log(this, "Reconnecting to Wi-Fi");
            wifi.reconnect(SSID);
        }
	}

    public static boolean isRunning() {
        return running.get();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Logger.log(this, "onTaskRemoved()");
        if (!settings.getBoolean("pref_notify_foreground", true)) {
            Logger.log("Stopping because of task removal");
            running.set(false);
        }
    }
}
