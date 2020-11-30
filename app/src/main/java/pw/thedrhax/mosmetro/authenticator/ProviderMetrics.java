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
import android.os.AsyncTask;

import java.io.IOException;
import java.util.HashMap;

import pw.thedrhax.mosmetro.BuildConfig;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.mosmetro.updater.BackgroundTask;
import pw.thedrhax.util.Logger;
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
            default:
                if (vars.containsKey("branch")) {
                    String branch = (String)vars.get("branch");
                    if (branch.endsWith("-fallback")) {
                        Logger.report("Unsuccessful MMV2 branch fallback (" + branch + ")");
                    }
                }

                return false;
        }

        WifiUtils wifi = new WifiUtils(p.context);

        final HashMap<String, String> params = new HashMap<>();

        params.put("version_name", Version.getVersionName());
        params.put("version_code", "" + Version.getVersionCode());
        params.put("build_branch", Version.getBranch());
        params.put("build_number", "" + Version.getBuildNumber());

        if (vars.containsKey("midsession")) {
            params.put("success", "midsession");
        } else {
            params.put("success", connected ? "true" : "false");
        }

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

                String STATISTICS_URL = p.settings.getString(
                        BackgroundTask.PREF_BACKEND_URL,
                        BuildConfig.API_URL_DEFAULT
                ) + BuildConfig.API_REL_STATISTICS;

                try {
                    new OkHttp(p.context).post(STATISTICS_URL, params);
                } catch (IOException ignored) {}

                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        if (System.currentTimeMillis() - 6*60*60*1000 > p.settings.getLong("pref_worker_timestamp", 0)) {
            new BackgroundTask(p.context).run();
        }

        return false;
    }
}
