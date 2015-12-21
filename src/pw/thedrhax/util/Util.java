package pw.thedrhax.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.MainActivity;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Util {
    // Convert Exception's printStackTrace() to String
    public static String exToStr (Exception ex) {
        StringWriter wr = new StringWriter();
        ex.printStackTrace(new PrintWriter(wr));
        return wr.toString();
    }

    // Create notification
    public static void notify (Context context, String title, String message, Intent intent) {
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(
                            context, 0, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )
                );

        NotificationManager nm = (NotificationManager)context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        nm.notify(0, builder.build());
    }

    public static void notify (Context context, String title, String message) {
        notify(context, title, message, new Intent(context, MainActivity.class));
    }
}
