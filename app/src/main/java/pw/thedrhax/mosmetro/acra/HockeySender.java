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

package pw.thedrhax.mosmetro.acra;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import org.acra.ReportField;
import org.acra.data.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;

import pw.thedrhax.mosmetro.BuildConfig;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.ParsedResponse;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Logger;

public class HockeySender implements ReportSender {
    @Override
    public void send(@NonNull Context context, @NonNull CrashReportData report) throws ReportSenderException {
        String log = createCrashLog(context, report);
        String url = "https://rink.hockeyapp.net/api/2/apps/" + BuildConfig.HOCKEYAPP_ID + "/crashes";

        try {
            Client client = new OkHttp(context);

            ParsedResponse res = client.post(url, new HashMap<String, String>() {{
                put("raw", log);
                put("userID", (String)report.get(ReportField.INSTALLATION_ID.toString()));
                put("contact", (String)report.get(ReportField.USER_EMAIL.toString()));
                put("description", (String)report.get(ReportField.USER_COMMENT.toString()));
            }});

            if (res.getResponseCode() != 201 && res.getResponseCode() != 400) {
                throw new Exception("Wrong response");
            }
        } catch (Exception ex) {
            Logger.log(Logger.LEVEL.DEBUG, ex);
            throw new ReportSenderException("Unable to send report", ex);
        }
    }

    private String createCrashLog(Context context, CrashReportData report) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean send_log = settings.getBoolean("pref_debug_last_log", true);

        String version = "unknown";
        try {
            JSONObject build_config = (JSONObject) report.get(ReportField.BUILD_CONFIG.toString());

            if (build_config != null) {
                Object branch = build_config.get("BRANCH_NAME");
                Object build = build_config.get("BUILD_NUMBER");

                if (branch != null && build != null) {
                    version = branch.toString() + " #" + build.toString();
                }
            }
        } catch (JSONException ignored) {}

        System.out.println(version);

        return "Package: " + report.get(ReportField.PACKAGE_NAME.toString()) + '\n' +
               "Version: " + version + '\n' +
               "Android: " + report.get(ReportField.ANDROID_VERSION.toString()) + '\n' +
               "Manufacturer: " + android.os.Build.MANUFACTURER + '\n' +
               "Model: " + report.get(ReportField.PHONE_MODEL.toString()) + '\n' +
               "Date: " + new Date() + '\n' +
               '\n' +
               report.get(ReportField.STACK_TRACE.toString()) + '\n' +
               "----\n\n" +
               (send_log ? report.get(ReportField.APPLICATION_LOG.toString()) : "log disabled");
    }
}