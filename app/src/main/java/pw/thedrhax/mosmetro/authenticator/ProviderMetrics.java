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
import android.content.SharedPreferences;
import android.os.Build;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.HashMap;
import java.util.Locale;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.SettingsActivity;
import pw.thedrhax.mosmetro.activities.SilentActionActivity;
import pw.thedrhax.mosmetro.services.ConnectionService;
import pw.thedrhax.mosmetro.updater.BackendRequest;
import pw.thedrhax.util.Notify;
import pw.thedrhax.util.UUID;
import pw.thedrhax.util.Version;
import pw.thedrhax.util.WifiUtils;

public class ProviderMetrics {
    private final Provider p;
    private final BackendRequest backreq;

    ProviderMetrics(Provider provider) {
        this.p = provider;
        this.backreq = new BackendRequest(provider.context);
    }

    private Long start_ts = null;

    public ProviderMetrics start() {
        start_ts = System.currentTimeMillis();
        return this;
    }

    @SuppressLint("StaticFieldLeak")
    @SuppressWarnings("unchecked")
    public boolean end(HashMap<String, Object> vars) {
        boolean connected = false;
        boolean error = false;
        boolean midsession = vars.containsKey("midsession");

        switch ((Provider.RESULT) vars.get("result")) {
            case CONNECTED:
                connected = true;
            case ALREADY_CONNECTED:
                break;

            case ERROR:
                error = true;
                break;

            default:
                return false;
        }

        WifiUtils wifi = new WifiUtils(p.context);

        final JSONObject version = new JSONObject();
        version.put("branch", Version.getBranch());
        version.put("build", Version.getBuildNumber());
        version.put("name", Version.getVersionName());
        version.put("code", Version.getVersionCode());
        version.put("android", Build.VERSION.SDK_INT);

        String ssid = wifi.getSSID();
        ssid = WifiUtils.UNKNOWN_SSID.equals(ssid) ? null : ssid;

        final JSONObject result = new JSONObject();
        result.put("provider", p.getName());
        result.put("segment", vars.get("segment"));
        result.put("branch", vars.get("branch"));
        result.put("switch", vars.get("switch"));
        result.put("duration", start_ts != null ? System.currentTimeMillis() - start_ts : null);
        result.put("ssid", ssid);
        result.put("success", connected);
        result.put("error", error);
        result.put("midsession", midsession);

        final JSONObject params = new JSONObject();
        params.put("timestamp", System.currentTimeMillis() / 1000L);
        params.put("version", version);
        params.put("result", result);

        JSONArray queue = getQueue(p.settings);
        queue.add(params);
        while (queue.size() > 100) {
            queue.remove(0);
        }
        saveQueue(p.settings, queue);

        // Run only if BackendWorker skipped two cycles
        if (backreq.getLastRun() > 12*60*60*1000) {
            backreq.run();
        }

        boolean pref_notify_donate = p.settings.getBoolean("pref_notify_donate", true);
        boolean pref_notify_donate_freq = p.settings.getBoolean("pref_notify_donate_freq", false);
        int stat_connections = p.settings.getInt("stat_connections", 0);

        if (pref_notify_donate && connected && !midsession) {
            stat_connections += 1;
            p.settings.edit().putInt("stat_connections", stat_connections).apply();

            if (stat_connections > 0 && stat_connections % (pref_notify_donate_freq ? 100 : 50) == 0) {
                Notify notify = new Notify(p.context)
                        .id(128)
                        .title(String.format(Locale.ENGLISH, p.context.getString(R.string.notification_donate_title), stat_connections))
                        .text(p.context.getString(R.string.notification_donate_text))
                        .icon(R.drawable.ic_notification_message_colored, R.drawable.ic_notification_message)
                        .cancelOnClick(true);

                if (!pref_notify_donate_freq) {
                    notify.addAction(p.context.getString(R.string.remind_less_frequently), PendingIntent.getActivity(
                            p.context, 129,
                            new Intent(p.context, SilentActionActivity.class)
                                    .setAction(SilentActionActivity.ACTION_TOGGLE_DONATE_REMINDER_FREQUENCY),
                            PendingIntent.FLAG_CANCEL_CURRENT
                    ));
                }

                notify.addAction(p.context.getString(R.string.do_not_show), PendingIntent.getActivity(
                        p.context, 128,
                        new Intent(p.context, SilentActionActivity.class)
                                .setAction(SilentActionActivity.ACTION_DISABLE_DONATE_REMINDER),
                        PendingIntent.FLAG_UPDATE_CURRENT
                ));

                notify.onClick(PendingIntent.getActivity(
                        p.context, 130,
                        new Intent(p.context, SettingsActivity.class)
                                .setAction(SettingsActivity.ACTION_DONATE),
                        PendingIntent.FLAG_UPDATE_CURRENT
                ));

                notify.show();
            }
        }

        return false;
    }

    public static JSONArray getQueue(SharedPreferences settings) {
        if (settings.contains("stats_queue")) {
            settings.edit().remove("stats_queue").apply();
        }

        String json = settings.getString("stats_queue_v2", "[]");

        try {
            return (JSONArray) new JSONParser().parse(json);
        } catch (ParseException ex) {
            return new JSONArray();
        }
    }

    public static synchronized void saveQueue(SharedPreferences settings, JSONArray queue) {
        settings.edit().putString("stats_queue_v2", queue.toJSONString()).apply();
    }
}
