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
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import org.acra.ReportField;
import org.acra.data.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import pw.thedrhax.mosmetro.BuildConfig;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Util;
import pw.thedrhax.util.Version;

public class HockeySender implements ReportSender {
    @Override
    public void send(@NonNull Context context, @NonNull CrashReportData report) throws ReportSenderException {
        String log = createCrashLog(context, report);
        String url = "https://rink.hockeyapp.net/api/2/apps/" + BuildConfig.HOCKEYAPP_ID + "/crashes";

        try {
            Client client = new OkHttp(context);
            client.post(url, new HashMap<String, String>() {{
                put("raw", log);
                put("userID", (String)report.get(ReportField.INSTALLATION_ID.toString()));
                put("contact", (String)report.get(ReportField.USER_EMAIL.toString()));
                put("description", (String)report.get(ReportField.USER_COMMENT.toString()));
            }});
        } catch (Exception ex) {
            Logger.log(Logger.LEVEL.DEBUG, ex);
            throw new ReportSenderException("Unable to send report", ex);
        }
    }

    private String readLastLog(Context context) {
        if (!PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("pref_debug_last_log", true)) {
            return "Last log was disabled by user.";
        }

        File last_log = new File(context.getFilesDir(), Logger.FILE_LAST_LOG);
        FileInputStream fis = null;
        String log;

        try {
            fis = new FileInputStream(last_log);
            log = Util.streamToString(fis);
        } catch (FileNotFoundException ex) {
            return "Log file not found!";
        } catch (IOException ex) {
            return "Can't read log file: " + Log.getStackTraceString(ex);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ignored) {}
        }

        return log;
    }

    private String createCrashLog(Context context, CrashReportData report) {
        return "Package: " + report.get(ReportField.PACKAGE_NAME.toString()) + '\n' +
               "Version: " + Version.getFormattedVersion() + '\n' +
               "Android: " + report.get(ReportField.ANDROID_VERSION.toString()) + '\n' +
               "Manufacturer: " + android.os.Build.MANUFACTURER + '\n' +
               "Model: " + report.get(ReportField.PHONE_MODEL.toString()) + '\n' +
               "Date: " + new Date() + '\n' +
               '\n' +
               report.get(ReportField.STACK_TRACE.toString()) + '\n' +
               '\n' +
               "----\n" +
               readLastLog(context);
    }
}