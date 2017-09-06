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

import android.os.AsyncTask;

import java.util.HashMap;
import java.util.Map;

import pw.thedrhax.mosmetro.BuildConfig;
import pw.thedrhax.mosmetro.httpclient.CachedRetriever;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.mosmetro.updater.NewsChecker;
import pw.thedrhax.util.Version;
import pw.thedrhax.util.WifiUtils;

class StatisticsTask implements Task {
    private Provider p;

    StatisticsTask(Provider provider) {
        this.p = provider;
    }

    @Override
    public boolean run(HashMap<String, Object> vars) {
        boolean connected;

        switch ((Provider.RESULT) vars.get("result")) {
            case CONNECTED: connected = true; break;
            case ALREADY_CONNECTED: connected = false; break;
            default: return false;
        }

        final Map<String,String> params = new HashMap<>();
        params.put("version", Version.getFormattedVersion());
        params.put("success", connected ? "true" : "false");
        params.put("ssid", new WifiUtils(p.context).getSSID());
        params.put("provider", p.getName());
        if (vars.get("captcha") != null) {
            params.put("captcha", (String) vars.get("captcha"));
            if ("entered".equals(vars.get("captcha"))) {
                params.put("captcha_image", (String) vars.get("captcha_image"));
                params.put("captcha_code", (String) vars.get("captcha_code"));
            }
        }

        new AsyncTask<Void,Void,Void>() {
            @Override
            protected Void doInBackground(Void... none) {
                String STATISTICS_URL = new CachedRetriever(p.context).get(
                        BuildConfig.API_URL_SOURCE, BuildConfig.API_URL_DEFAULT,
                        CachedRetriever.Type.URL
                ) + BuildConfig.API_REL_STATISTICS;

                try {
                    new OkHttp(p.context).post(STATISTICS_URL, params);
                } catch (Exception ignored) {}

                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        if (p.settings.getBoolean("pref_notify_news", true)) {
            new NewsChecker(p.context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        return false;
    }
}
