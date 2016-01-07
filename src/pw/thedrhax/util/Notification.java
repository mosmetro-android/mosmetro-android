package pw.thedrhax.util;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.os.Build;
import pw.thedrhax.mosmetro.R;

public class Notification {
    private Context context;
    private android.app.Notification.Builder builder;
    private NotificationManager nm;

    private int id = 0;

    public Notification (Context context) {
        this.context = context;
        this.builder = new android.app.Notification.Builder(context);
        this.nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        builder = builder.setSmallIcon(R.drawable.ic_notification); // default icon
        builder = builder.setAutoCancel(true); // delete notification on press
    }

    public Notification setTitle (String title) {
        builder = builder.setContentTitle(title);
        return this;
    }

    public Notification setText (String text) {
        builder = builder.setContentText(text);

        if (Build.VERSION.SDK_INT >= 16) // show expandable text on newer devices
            builder = builder.setStyle(new android.app.Notification.BigTextStyle().bigText(text));
        return this;
    }

    public Notification setIntent (Intent intent) {
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder = builder.setContentIntent(pIntent);
        return this;
    }

    public Notification setId (int id) {
        this.id = id;
        return this;
    }

    public Notification setContinuous () {
        if (Build.VERSION.SDK_INT >= 14)
            builder = builder.setProgress(0, 0, true);
        return this;
    }

    public Notification setProgress (int progress) {
        if (Build.VERSION.SDK_INT >= 14)
            builder = builder.setProgress(100, progress, false);
        return this;
    }

    public void show() {
        if (Build.VERSION.SDK_INT >= 16) {
            nm.notify(id, builder.build());
        } else { // support older devices
            nm.notify(id, builder.getNotification());
        }
    }

    public void hide() {
        nm.cancel(id);
    }
}
