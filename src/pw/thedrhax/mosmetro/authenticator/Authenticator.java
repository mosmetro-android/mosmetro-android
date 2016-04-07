package pw.thedrhax.mosmetro.authenticator;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import pw.thedrhax.mosmetro.authenticator.networks.AURA;
import pw.thedrhax.mosmetro.authenticator.networks.MosMetro;
import pw.thedrhax.mosmetro.httpclient.CachedRetriever;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Logger;

import java.util.HashMap;
import java.util.Map;

public abstract class Authenticator {
    public static final Class<? extends Authenticator>[] SUPPORTED_NETWORKS =
            new Class[] {MosMetro.class, AURA.class};

    // Result state
    public static final int STATUS_CONNECTED = 0;
    public static final int STATUS_ALREADY_CONNECTED = 1;
    public static final int STATUS_NOT_REGISTERED = 2;
    public static final int STATUS_ERROR = 3;

    // Network check state
    public static final int CHECK_CONNECTED = 0;
    public static final int CHECK_WRONG_NETWORK = 1;
    public static final int CHECK_NOT_CONNECTED = 2;

	protected Logger logger;
    protected Client client;

    // Device info
    private Context context;
    private boolean automatic;

    public Authenticator (Context context, boolean automatic) {
        logger = new Logger();
        client = new OkHttp();

        this.context = context;
        this.automatic = automatic;
    }

    public static Authenticator choose (Context context, boolean automatic, String SSID) {
        // Trying to match one of Authenticators for this SSID
        Class<? extends Authenticator> result_class = null;
        for (Class<? extends Authenticator> network : SUPPORTED_NETWORKS) {
            try {
                String class_ssid = (String) network.getField("SSID").get(network);
                if ((SSID.equals(class_ssid))) {
                    result_class = network; break;
                }
            } catch (Exception ignored) {}
        }
        if (result_class == null) return null;

        // Make instance of matched Authenticator
        Authenticator result;
        try {
            result = result_class
                    .getConstructor(Context.class, boolean.class)
                    .newInstance(context, automatic);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return result;
    }

    public static Authenticator choose (Context context, boolean automatic) {
        // Get SSID from WifiManager
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String SSID = manager.getConnectionInfo().getSSID();

        return choose(context, automatic, SSID);
    }

    public abstract String getSSID();

    public int start() {
        logger.debug("Версия приложения: " + getVersion());
        int result = connect();

        if (result <= STATUS_ALREADY_CONNECTED)
            submit_info(result);

        return result;
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
        String STATISTICS_URL = new CachedRetriever(context)
                .get(CachedRetriever.BASE_URL_SOURCE, "http://wi-fi.metro-it.com") + "/check.php";

        Map<String,String> params = new HashMap<String, String>();
        params.put("version", getVersion());
        params.put("automatic", automatic ? "1" : "0");
        params.put("connected", result == STATUS_CONNECTED ? "1" : "0");
        params.put("ssid", getSSID().replace("\"", ""));

        try {
            new OkHttp().post(STATISTICS_URL, params);
        } catch (Exception ignored) {}
    }
}
