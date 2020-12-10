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

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.SettingsActivity;
import pw.thedrhax.util.Notify;

public class ReceiverService extends Service {
    public static final int NOTIFY_ID = 123;

    private Notify notify;
    private NetworkReceiver receiver;
    private BroadcastReceiver connection_receiver;

    @Override
    public void onCreate() {
        super.onCreate();

        notify = new Notify(this)
                .title(getString(R.string.receiver_service_title))
                .text(getString(R.string.reveiver_service_summary))
                .icon(R.drawable.ic_notification_success_colored, R.drawable.ic_notification_success)
                .onClick(PendingIntent.getActivity(
                        this, 2,
                        new Intent(this, SettingsActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT
                ))
                .id(NOTIFY_ID)
                .priority(-2)
                .locked(true);
        
        receiver = new NetworkReceiver().setDynamic(true);
        connection_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getBooleanExtra(ConnectionService.EXTRA_RUNNING, false)) {
                    notify.hide();
                } else {
                    notify.show();
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!ConnectionService.isRunning()) {
            notify.show();
        }

        registerReceiver(
                connection_receiver,
                new IntentFilter(ConnectionService.ACTION_EVENT)
        );

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(receiver, filter);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        unregisterReceiver(connection_receiver);

        startService(
                new Intent(this, ConnectionService.class)
                        .setAction(ConnectionService.ACTION_STOP)
        );

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
