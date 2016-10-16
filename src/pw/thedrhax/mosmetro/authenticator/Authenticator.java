package pw.thedrhax.mosmetro.authenticator;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import org.jsoup.Jsoup;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.networks.AURA;
import pw.thedrhax.mosmetro.authenticator.networks.MosMetro;
import pw.thedrhax.mosmetro.authenticator.networks.MosGorTrans;
import pw.thedrhax.mosmetro.httpclient.CachedRetriever;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Logger;

import java.util.HashMap;
import java.util.Map;

public abstract class Authenticator {
    public static final Class<? extends Authenticator>[] SUPPORTED_NETWORKS =
            new Class[] {MosMetro.class, AURA.class, MosGorTrans.class};

    // Result state
    public static final int STATUS_CONNECTED = 0;
    public static final int STATUS_ALREADY_CONNECTED = 1;
    public static final int STATUS_NOT_REGISTERED = 2;
    public static final int STATUS_ERROR = 3;
    public static final int STATUS_INTERRUPTED = 4;

    // Network check state
    public static final int CHECK_CONNECTED = 0;
    public static final int CHECK_WRONG_NETWORK = 1;
    public static final int CHECK_NOT_CONNECTED = 2;

	protected Logger logger;
    protected Client client;
    protected boolean stopped;

    // Device info
    protected Context context;
    protected SharedPreferences settings;
    protected int pref_retry_count;

    public Authenticator (Context context) {
        logger = new Logger();
        client = new OkHttp();

        this.context = context;
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);
        this.pref_retry_count = Integer.parseInt(settings.getString("pref_retry_count", "3"));
    }

    public abstract String getSSID();

    public int start() {
        stopped = false;

        logger.log(Logger.LEVEL.DEBUG,
                   String.format(context.getString(R.string.version), getVersion()));
        int result = connect();

        if (stopped) return result;

        if (result <= STATUS_ALREADY_CONNECTED)
            submit_info(result);

        return result;
    }

    public void stop() {
        stopped = true;
    }

    /*
     * Logging
     */

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /*
     * Connection sequence
     */

    public abstract int isConnected();

    protected abstract int connect();

    /*
     * Progress reporting
     */

    protected ProgressListener progressListener = new ProgressListener() {
        @Override
        public void onProgressUpdate(int progress) {

        }
    };

    public void setProgressListener (ProgressListener listener) {
        progressListener = listener;
    }

    public interface ProgressListener {
        void onProgressUpdate(int progress);
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
        String STATISTICS_URL = Jsoup.parse(
                new CachedRetriever(context)
                        .get(CachedRetriever.BASE_URL_SOURCE, "http://wi-fi.metro-it.com")
        ).getElementsByTag("body").html() + "/check.php";

        Map<String,String> params = new HashMap<String, String>();
        params.put("version", getVersion());
        params.put("connected", result == STATUS_CONNECTED ? "1" : "0");
        params.put("ssid", getSSID());

        try {
            new OkHttp().post(STATISTICS_URL, params);
        } catch (Exception ignored) {}
    }
}