package pw.thedrhax.mosmetro.acra;

import android.content.Context;
import android.support.annotation.NonNull;

import org.acra.ReportField;
import org.acra.data.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

import java.util.Date;
import java.util.HashMap;

import pw.thedrhax.mosmetro.BuildConfig;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Version;

public class HockeySender implements ReportSender {
    @Override
    public void send(@NonNull Context context, @NonNull CrashReportData report) throws ReportSenderException {
        String log = createCrashLog(report);
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

    private String createCrashLog(CrashReportData report) {
        return "Package: " + report.get(ReportField.PACKAGE_NAME.toString()) + '\n' +
               "Version: " + Version.getFormattedVersion() + '\n' +
               "Android: " + report.get(ReportField.ANDROID_VERSION.toString()) + '\n' +
               "Manufacturer: " + android.os.Build.MANUFACTURER + '\n' +
               "Model: " + report.get(ReportField.PHONE_MODEL.toString()) + '\n' +
               "Date: " + new Date() + '\n' +
               '\n' +
               report.get(ReportField.STACK_TRACE.toString());
    }
}