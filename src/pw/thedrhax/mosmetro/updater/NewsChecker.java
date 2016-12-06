package pw.thedrhax.mosmetro.updater;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.httpclient.CachedRetriever;
import pw.thedrhax.util.Notification;
import pw.thedrhax.util.Version;

public class NewsChecker {
    private Context context;
    private SharedPreferences settings;
    private boolean pref_colored_icons;

    public NewsChecker(Context context) {
        this.context = context;

        settings = PreferenceManager.getDefaultSharedPreferences(context);
        pref_colored_icons = (Build.VERSION.SDK_INT <= 20)
                || settings.getBoolean("pref_notify_alternative", false);
    }

    public void check() {
        String content = new CachedRetriever(context).get(URLs.NEWS_URL, 30*60, "{}");;

        JSONObject data;
        try {
            data = (JSONObject) new JSONParser().parse(content);
        } catch (ParseException ex) {
            return;
        }

        long id, max_version;
        String title, message;
        Uri url;
        try {
            id = Integer.parseInt((String)data.get("id"));
            max_version = Integer.parseInt((String)data.get("max_version"));
            title = (String)data.get("title");
            message = (String)data.get("message");
            url = Uri.parse((String)data.get("url"));
        } catch (Exception ex) {
            return;
        }

        if (max_version < new Version(context).getVersionCode())
            return;

        if (settings.getLong("pref_notify_news_id", 0) >= id)
            return;

        new Notification(context)
                .setCancellable(true)
                .setId(255)
                .setIcon(pref_colored_icons ?
                        R.drawable.ic_notification_message_colored :
                        R.drawable.ic_notification_message)
                .setIntent(new Intent(Intent.ACTION_VIEW).setData(url))
                .setTitle(title)
                .setText(message)
                .show();

        settings.edit()
                .putLong("pref_notify_news_id", id)
                .apply();
    }
}
