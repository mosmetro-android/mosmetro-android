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

package pw.thedrhax.mosmetro.updater;

import java.io.IOException;
import java.util.Map;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.jayway.jsonpath.DocumentContext;

import net.minidev.json.JSONArray;

import pw.thedrhax.mosmetro.BuildConfig;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.SafeViewActivity;
import pw.thedrhax.mosmetro.activities.SettingsActivity;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Notify;
import pw.thedrhax.util.Util;

public class BackendRequest {
    public static final String PREF_BACKEND_URL = "backend_url";

    private Context context;
    private SharedPreferences settings;
    private Client client;

    public BackendRequest(Context context) {
        this.context = context;
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);
        this.client = new OkHttp(context);
    }

    private boolean checkNews(DocumentContext data) {
        JSONArray news = data.read("$.news[?(@.max_builds['" + BuildConfig.BRANCH_NAME + "'] >= " + BuildConfig.BUILD_NUMBER + ")]");

        Map<String,Object> post;

        try {
            post = (Map<String,Object>) news.get(0);
        } catch (ClassCastException | IndexOutOfBoundsException ex) {
            return false;
        }

        if (post == null) {
            return false;
        }

        long id;
        String title, message, url;
        try {
            id = (Integer)post.get("id");
            title = (String)post.get("title");
            message = (String)post.get("message");
            url = (String)post.get("url");
        } catch (ClassCastException | NullPointerException ex) {
            return false;
        }

        if (settings.getLong("pref_notify_news_id", 0) >= id)
            return false;

        settings.edit().putLong("pref_notify_news_id", id).apply();

        new Notify(context).id(255)
                .icon(R.drawable.ic_notification_message_colored,
                      R.drawable.ic_notification_message)
                .onClick(PendingIntent.getActivity(context, 255,
                        new Intent(context, SafeViewActivity.class)
                                .putExtra("data", url),
                        PendingIntent.FLAG_UPDATE_CURRENT
                ))
                .title(title)
                .text(message)
                .cancelOnClick(true)
                .show();

        return true;
    }

    private void checkUpdates(DocumentContext data) {
        UpdateChecker.Result result = new UpdateChecker(context).check(data);

        if (result == null)
            return;

        if (!result.hasUpdate() || result.getBranch() == null)
            return;

        UpdateChecker.Branch branch = result.getBranch();

        // Show notification only once for each build
        if (settings.getString("pref_updater_ignore_notification", "").equals(branch.id()))
            return;

        settings.edit().putString("pref_updater_ignore_notification", branch.id()).apply();

        Notify notify = new Notify(context)
                .id(3)
                .title(context.getString(R.string.update_available))
                .text(branch.message)
                .icon(R.drawable.ic_notification_message,
                        R.drawable.ic_notification_message_colored)
                .cancelOnClick(true)
                .onClick(PendingIntent.getActivity(
                        context, 252,
                        new Intent(context, SettingsActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT));

        notify.show();
    }

    public DocumentContext getCachedData(boolean force) {
        if (getLastRun() > 6*60*60*1000 || force) {
            try {
                return Util.jsonpath(settings.getString("worker_cache_data", ""));
            } catch (java.text.ParseException ignored) {}
        }

        return Util.JSONPATH_EMPTY;
    }

    public DocumentContext getData(boolean force) {
        DocumentContext res = Util.JSONPATH_EMPTY;

        if (!force) {
            res = getCachedData(false);

            if (res != Util.JSONPATH_EMPTY) {
                return res;
            }
        }

        try {
            res = client.get(BuildConfig.API_URL_DATA).execute().jsonpath();
        } catch (IOException ignored) {}

        if (res != Util.JSONPATH_EMPTY) {
            settings.edit()
                    .putLong("worker_timestamp", System.currentTimeMillis())
                    .putString("worker_cache_data", res.jsonString())
                    .apply();
        }

        return res;
    }

    public long getLastRun() {
        long last_ts = settings.getLong("worker_timestamp", 0);
        long ts = System.currentTimeMillis();
        return ts - last_ts;
    }

    public boolean run() {
        DocumentContext data = getData(false);

        if (data == Util.JSONPATH_EMPTY) {
            return false;
        }

        if (settings.getBoolean("pref_notify_news", true)) {
            checkNews(data);
        }

        if (settings.getBoolean("pref_updater_enabled", true)) {
            checkUpdates(data);
        }

        return true;
    }
}
