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
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Notification;

public class NewsChecker {
    private Context context;
    private SharedPreferences settings;
    private int pref_retry_count;
    private boolean pref_colored_icons;

    private Client client;

    public NewsChecker(Context context) {
        this.context = context;

        settings = PreferenceManager.getDefaultSharedPreferences(context);
        pref_retry_count = Integer.parseInt(settings.getString("pref_retry_count", "3"));
        pref_colored_icons = (Build.VERSION.SDK_INT <= 20) || settings.getBoolean("pref_notify_alternative", false);

        client = new OkHttp();
    }

    public boolean check() {
        try {
            client.get(URLs.NEWS_URL, null, pref_retry_count);
        } catch (Exception ex) {
            return false;
        }

        JSONParser parser = new JSONParser();
        JSONObject data;
        try {
            data = (JSONObject)parser.parse(client.getPage());
        } catch (ParseException ex) {
            return false;
        }

        long id;
        String title, message;
        Uri url;
        try {
            id = (Long)data.get("id");
            title = (String)data.get("title");
            message = (String)data.get("message");
            url = Uri.parse((String)data.get("url"));
        } catch (ClassCastException ex) {
            return false;
        } catch (NullPointerException ex) {
            return false;
        }

        if (settings.getLong("pref_notify_news_id", 0) >= id)
            return false;

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

        return true;
    }
}
