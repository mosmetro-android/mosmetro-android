package pw.thedrhax.mosmetro.authenticator;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import pw.thedrhax.httpclient.HttpClient;

import java.io.IOException;

public class AuthenticatorStat extends Authenticator {
    private static final String STATISTICS_URL = "http://thedrhax.pw/mosmetro/check.php";

    private Context context;
    private boolean automatic;

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
        logger.debug("Версия приложения: " + getVersion());
        int result = super.connect();

        if (result <= STATUS_ALREADY_CONNECTED)
            submit_info(result);

        return result;
    }

    private void submit_info (int result) {
        StringBuilder params = new StringBuilder();

        params.append("version=").append(getVersion()).append("&");
        params.append("automatic=").append(automatic ? "1" : "0").append("&");
        params.append("connected=").append(result == STATUS_CONNECTED ? "1" : "0");

        try {
            new HttpClient().navigate(STATISTICS_URL, params.toString());
        } catch (IOException ignored) {}
    }
}
