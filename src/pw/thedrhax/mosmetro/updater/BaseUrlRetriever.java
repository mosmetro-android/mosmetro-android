package pw.thedrhax.mosmetro.updater;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;

public class BaseUrlRetriever {
    private static final String BASE_URL_SOURCE = "https://thedrhax.github.io/mosmetro-android/base-url";

    private SharedPreferences settings;

    public BaseUrlRetriever(Context context) {
        settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getBaseUrl() {
        if (settings.getString("base_url_cache", "").isEmpty() ||
            settings.getLong("base_url_timestamp", 0) + 60*60 < System.currentTimeMillis() / 1000) {
            try {
                String base_url = new OkHttpClient().newCall(
                        new Request.Builder().url(BASE_URL_SOURCE).get().build()
                ).execute().body().string().trim();

                settings
                        .edit()
                        .putLong("base_url_timestamp", System.currentTimeMillis() / 1000L)
                        .apply();

                return base_url;
            } catch (IOException ignored) {}
        }

        return settings.getString("base_url_cache", "http://thedrhax.pw/mosmetro");
    }
}
