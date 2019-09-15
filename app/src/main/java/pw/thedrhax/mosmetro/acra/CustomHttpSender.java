package pw.thedrhax.mosmetro.acra;

import org.acra.ReportField;
import org.acra.config.CoreConfiguration;
import org.acra.data.CrashReportData;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;
import org.acra.sender.ReportSenderException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import pw.thedrhax.util.Logger;

class CustomHttpSender extends HttpSender {
    public CustomHttpSender(CoreConfiguration config, Method method, StringFormat type, String formUri) {
        super(config, method, type, formUri);
    }

    public CustomHttpSender(CoreConfiguration config, Method method, StringFormat type) {
        super(config, method, type);
    }

    @Override
    public void send(Context context, CrashReportData report) throws ReportSenderException {
        JSONObject build_config = (JSONObject) report.get(ReportField.BUILD_CONFIG.toString());
        if (build_config == null) return;

        try {
            Object branch = build_config.get("BRANCH_NAME");
            Object build = build_config.get("BUILD_NUMBER");

            if (branch != null && build != null) {
                report.put(ReportField.APP_VERSION_NAME, branch.toString() + " #" + build.toString());
                report.put(ReportField.APP_VERSION_CODE, Integer.parseInt(build.toString()));
            }
        } catch (JSONException|NumberFormatException ignored) {}

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        if (!settings.getBoolean("pref_debug_last_log", true)) {
            report.put(ReportField.APPLICATION_LOG, (String) null);
        } else {
            String log = (String) report.get(ReportField.APPLICATION_LOG.toString());
            if (log != null) {
                int cut = log.lastIndexOf(Logger.CUT);
                if (cut != -1) {
                    log = log.substring(cut + Logger.CUT.length() + 1);
                    report.put(ReportField.APPLICATION_LOG, log);
                }
            }
        }

        super.send(context, report);
    }
}