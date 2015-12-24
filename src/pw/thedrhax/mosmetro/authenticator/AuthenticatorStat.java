package pw.thedrhax.mosmetro.authenticator;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import pw.thedrhax.httpclient.HttpClient;

public class AuthenticatorStat extends Authenticator {
    private static final String INTERNET_CHECK_URL = "http://thedrhax.pw/mosmetro/check.php";
    private static final String INTERNET_CHECK_KEY = "2fv3bYW6x92V3Y7gM5FfT7Wmh";

    private StringBuilder params;

    public AuthenticatorStat(Context context, boolean isAutomatic) {
        super();

        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException ignored) {}

        params = new StringBuilder();
        if (pInfo != null)
            params.append(String.format("version=%s-%d&", pInfo.versionName, pInfo.versionCode));
        params.append(isAutomatic ? "TRUE" : "FALSE");
    }

    public boolean isConnected() {
        try {
            return new HttpClient()
                    .navigate(INTERNET_CHECK_URL, params.toString())
                    .getContent().contains(INTERNET_CHECK_KEY);
        } catch (Exception ex) {
            return false;
        }
    }
}
