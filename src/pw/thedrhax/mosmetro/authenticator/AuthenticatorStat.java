package pw.thedrhax.mosmetro.authenticator;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import pw.thedrhax.mosmetro.httpclient.BetterDns;
import pw.thedrhax.mosmetro.httpclient.CachedRetriever;

import java.io.IOException;

public abstract class AuthenticatorStat extends Authenticator {
    private Context context;
    private boolean automatic;

    public AuthenticatorStat(Context context, boolean automatic) {
        super();
        this.context = context;
        this.automatic = automatic;
    }

    /*
     * System info
     */

    private String getVersion() {
        PackageInfo pInfo;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName + "-" + pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException ex) {
            return "";
        }
    }

    private void submit_info (int result) {
        final String STATISTICS_URL = new CachedRetriever(context)
                .get(CachedRetriever.BASE_URL_SOURCE, "http://wi-fi.metro-it.com") + "/check.php";

        RequestBody body = new FormBody.Builder()
                    .add("version", getVersion())
                    .add("automatic", automatic ? "1" : "0")
                    .add("connected", result == STATUS_CONNECTED ? "1" : "0")
                    .build();

        try {
            new OkHttpClient.Builder().dns(new BetterDns(context)).build().newCall(
                    new Request.Builder().url(STATISTICS_URL).post(body).build()
            ).execute();
        } catch (IOException ignored) {}
    }

    @Override
    public int start() {
        logger.debug("Версия приложения: " + getVersion());
        int result = super.start();

        if (result <= STATUS_ALREADY_CONNECTED)
            submit_info(result);

        return result;
    }
}
