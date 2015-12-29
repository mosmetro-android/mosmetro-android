package pw.thedrhax.mosmetro.authenticator;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import pw.thedrhax.httpclient.HttpClient;

import java.io.IOException;

public class AuthenticatorStat extends Authenticator {
    private static final String INTERNET_CHECK_URL = "http://thedrhax.pw/mosmetro/check.php";
    private static final String INTERNET_CHECK_KEY = "2fv3bYW6x92V3Y7gM5FfT7Wmh";
    private static final String REPORT_URL = "http://thedrhax.pw/mosmetro/report.php";

    private Context context;
    private boolean isAutomatic;

    public AuthenticatorStat(Context context, boolean isAutomatic) {
        super();
        this.context = context;
        this.isAutomatic = isAutomatic;
    }

    private String getVersion() {
        PackageInfo pInfo;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName + "-" + pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException ex) {
            return "unknown";
        }
    }

    public boolean isConnected() {
        StringBuilder params = new StringBuilder();

        params.append("version=").append(getVersion()).append("&");
        params.append("isAutomatic=").append(isAutomatic ? "TRUE" : "FALSE");
        try {
            return new HttpClient()
                    .navigate(INTERNET_CHECK_URL, params.toString())
                    .getContent().contains(INTERNET_CHECK_KEY);
        } catch (Exception ex) {
            return false;
        }
    }

    public void report(final String log, final String message) {
        new Thread(new Runnable() {
            public void run() {
                StringBuilder params = new StringBuilder();
                params.append("log=").append(Uri.encode(log)).append("&");
                params.append("message=").append(Uri.encode(message)).append("&");
                params.append("version=").append(getVersion());

                try {
                    new HttpClient().navigate(REPORT_URL, params.toString());
                } catch (IOException ignored) {}
            }
        }).start();
    }
}
