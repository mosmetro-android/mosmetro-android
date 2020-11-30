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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Patterns;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import pw.thedrhax.mosmetro.BuildConfig;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.SafeViewActivity;
import pw.thedrhax.mosmetro.activities.SettingsActivity;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.mosmetro.updater.UpdateCheckTask;
import pw.thedrhax.util.Notify;
import pw.thedrhax.util.Version;

public class ScheduledWorker extends Worker {
    public static final String PREF_BACKEND_URL = "backend_url";

    private Context context;
    private SharedPreferences settings;
    private Client client;

    public ScheduledWorker(Context context, WorkerParameters params) {
        super(context, params);
        this.context = context;
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);
        this.client = new OkHttp(context);
    }

    private boolean checkBackendUrl() {
        String url;

        try {
            url = client.get(BuildConfig.API_URL_SOURCE, null).getPage();
        } catch (IOException ex) {
            return false;
        }

        if (url == null || url.isEmpty())
            return false;
        
        if (!Patterns.WEB_URL.matcher(url).matches())
            return false;
        
        settings.edit()
                .putString(PREF_BACKEND_URL, url)
                .apply();

        return true;
    }

    private boolean checkNews() {
        JSONObject data;

        try {
            data = client.get(BuildConfig.NEWS_URL, null).json();
        } catch (IOException | ParseException ex) {
            return false;
        }

        long id, max_version;
        String title, message, url;
        try {
            id = (Long)data.get("id");
            max_version = (Long)data.get("max_version");
            title = (String)data.get("title");
            message = (String)data.get("message");
            url = (String)data.get("url");
        } catch (ClassCastException | NullPointerException ex) {
            return false;
        }

        if (max_version < Version.getVersionCode())
            return false;

        if (settings.getLong("pref_notify_news_id", 0) >= id)
            return false;

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

        settings.edit()
                .putLong("pref_notify_news_id", id)
                .apply();

        return true;
    }

    private boolean checkUpdates() {
        UpdateCheckTask updater = new UpdateCheckTask(context).force(false);
        UpdateCheckTask.Result result = updater.check();

        if (result == null)
            return false;

        if (!result.hasUpdate() || result.getBranch() == null)
            return true;

        UpdateCheckTask.Branch branch = result.getBranch();

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
        return true;
    }

    @Override
    public Result doWork() {
        checkBackendUrl();

        if (settings.getBoolean("pref_notify_news", true)) {
            checkNews();
        }

        if (settings.getBoolean("pref_updater_enabled", true)) {
            if (!checkUpdates()) return Result.retry();
        }

        return Result.success();
    }

    public static void configure(Context context) {
        Constraints constraints = new Constraints();
        constraints.setRequiredNetworkType(NetworkType.UNMETERED);

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ScheduledWorker.class, 3, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(10, TimeUnit.MINUTES)
                .addTag("ScheduledWorker")
                .build();

        WorkManager manager = WorkManager.getInstance(context);

        manager.enqueueUniquePeriodicWork(
            "ScheduledWorker",
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        );
    }
}
