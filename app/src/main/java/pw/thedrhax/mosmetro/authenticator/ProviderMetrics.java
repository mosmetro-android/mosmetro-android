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

package pw.thedrhax.mosmetro.authenticator;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.AsyncTask;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.HashMap;

import pw.thedrhax.mosmetro.BuildConfig;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.SettingsActivity;
import pw.thedrhax.mosmetro.httpclient.CachedRetriever;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.mosmetro.updater.NewsChecker;
import pw.thedrhax.mosmetro.updater.UpdateCheckTask;
import pw.thedrhax.util.Notify;
import pw.thedrhax.util.Version;
import pw.thedrhax.util.WifiUtils;

class ProviderMetrics {
    private Provider p;

    ProviderMetrics(Provider provider) {
        this.p = provider;
    }

    private Long start_ts = null;

    public ProviderMetrics start() {
        start_ts = System.currentTimeMillis();
        return this;
    }

    @SuppressLint("StaticFieldLeak")
    public boolean end(HashMap<String, Object> vars) {
        boolean connected;

        switch ((Provider.RESULT) vars.get("result")) {
            case CONNECTED: connected = true; break;
            case ALREADY_CONNECTED: connected = false; break;
            default: return false;
        }

        WifiUtils wifi = new WifiUtils(p.context);

        final HashMap<String, String> params = new HashMap<>();

        params.put("version_name", Version.getVersionName());
        params.put("version_code", "" + Version.getVersionCode());
        params.put("build_branch", Version.getBranch());
        params.put("build_number", "" + Version.getBuildNumber());

        params.put("success", connected ? "true" : "false");
        params.put("ssid", wifi.getSSID());
        params.put("provider", p.getName());

        String provider = p.getName();

        if (start_ts != null) {
            params.put("duration", "" + (System.currentTimeMillis() - start_ts));
        }

        if (vars.containsKey("switch")) {
            provider = (String) vars.get("switch");
            params.put("switch", provider);
        }

        if (vars.containsKey("segment")) {
            params.put("segment", (String) vars.get("segment"));
        }

        if (vars.containsKey("branch")) {
            params.put("branch", (String) vars.get("branch"));
        }

        new AsyncTask<Void,Void,Void>() {
            @Override
            protected Void doInBackground(Void... none) {
                if (!p.random.delay(p.running)) return null;

                String STATISTICS_URL = new CachedRetriever(p.context).get(
                        BuildConfig.API_URL_SOURCE, BuildConfig.API_URL_DEFAULT,
                        CachedRetriever.Type.URL
                ) + BuildConfig.API_REL_STATISTICS;

                try {
                    new OkHttp(p.context).post(STATISTICS_URL, params);
                } catch (IOException ignored) {}

                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        if (p.settings.getBoolean("pref_notify_news", true)) {
            new NewsChecker(p.context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        if (p.settings.getBoolean("pref_updater_enabled", true)) {
            new UpdateCheckTask(p.context) {
                @Override
                public void result(boolean hasUpdate, @Nullable Branch current_branch) {
                    if (!hasUpdate || current_branch == null) return;

                    Notify notify = new Notify(p.context)
                            .id(3)
                            .title(p.context.getString(R.string.update_available))
                            .text(current_branch.message)
                            .icon(R.drawable.ic_notification_message,
                                  R.drawable.ic_notification_message_colored)
                            .cancelOnClick(true)
                            .onClick(PendingIntent.getActivity(
                                    p.context, 252,
                                    new Intent(p.context, SettingsActivity.class),
                                    PendingIntent.FLAG_UPDATE_CURRENT));

                    notify.show();
                }
            }.force(false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        return false;
    }
}
