package pw.thedrhax.mosmetro.authenticator;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import pw.thedrhax.httpclient.HttpClient;
import pw.thedrhax.util.Logger;

import java.io.IOException;

public class AuthenticatorStat extends Authenticator {
    private static final String INTERNET_CHECK_URL = "http://thedrhax.pw/mosmetro/check.php";
    private static final String INTERNET_CHECK_KEY = "2fv3bYW6x92V3Y7gM5FfT7Wmh";
    private static final String REPORT_URL = "http://thedrhax.pw/mosmetro/report.php";

    private Context context;
    private boolean automatic;

    private boolean first_check = true;

    public AuthenticatorStat(Context context, boolean automatic) {
        super();
        this.context = context;
        this.automatic = automatic;
    }

    private String getVersion() {
        PackageInfo pInfo;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName + "-" + pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException ex) {
            return "";
        }
    }

    @Override
    public int connect() {
        first_check = true;
        return super.connect();
    }

    @Override
    public boolean isConnected() {
        StringBuilder params = new StringBuilder();

        params.append("version=").append(getVersion()).append("&");
        params.append("automatic=").append(automatic ? "1" : "0").append("&");
        params.append("connected=").append(first_check ? "0" : "1");
        first_check = false;
        try {
            return new HttpClient()
                    .navigate(INTERNET_CHECK_URL, params.toString())
                    .getContent().contains(INTERNET_CHECK_KEY);
        } catch (Exception ex) {
            logger.debug(ex);
            return false;
        }
    }

    public void report(final Logger log, final String message) {
        new Thread(new Runnable() {
            public void run() {
                StringBuilder params = new StringBuilder();
                params.append("log=").append(Uri.encode(log.getDebug())).append("&");
                params.append("message=").append(Uri.encode(message)).append("&");
                params.append("version=").append(getVersion());

                try {
                    new HttpClient().navigate(REPORT_URL, params.toString());
                } catch (IOException ignored) {}
            }
        }).start();
    }
}
