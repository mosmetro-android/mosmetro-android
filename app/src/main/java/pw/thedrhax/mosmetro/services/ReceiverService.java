package pw.thedrhax.mosmetro.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.annotation.Nullable;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.SettingsActivity;
import pw.thedrhax.util.Notify;

public class ReceiverService extends Service {
    public static final int NOTIFY_ID = 123;

    private Notify notify;

    private NetworkReceiver receiver;

    private BroadcastReceiver connection_service_receiver;

    @Override
    public void onCreate() {
        super.onCreate();
        notify = new Notify(this)
                .title(getString(R.string.receiver_service_title))
                .text(getString(R.string.receiver_service_summary))
                .icon(R.drawable.ic_notification_success_colored, R.drawable.ic_notification_success)
                .onClick(PendingIntent.getActivity(
                        this, 2,
                        new Intent(this, SettingsActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT
                ))
                .id(NOTIFY_ID)
                .priority(-2)
                .locked(true);

        receiver = new NetworkReceiver();

        connection_service_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!intent.getBooleanExtra(ConnectionService.EXTRA_RUNNING, false)) {
                    notify.show();
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notify.show();

        registerReceiver(
                connection_service_receiver,
                new IntentFilter(ConnectionService.ACTION_EVENT)
        );

        IntentFilter network_filter = new IntentFilter();
        network_filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        network_filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(receiver, network_filter);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        startService(
                new Intent(this, ConnectionService.class)
                        .setAction(ConnectionService.ACTION_STOP)
        );

        unregisterReceiver(receiver);
        unregisterReceiver(connection_service_receiver);

        notify.hide();
        super.onDestroy();
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
