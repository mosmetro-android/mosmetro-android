package pw.thedrhax.mosmetro.authenticator;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.updater.URLs;
import pw.thedrhax.mosmetro.authenticator.networks.AURA;
import pw.thedrhax.mosmetro.authenticator.networks.MosMetro;
import pw.thedrhax.mosmetro.httpclient.CachedRetriever;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.mosmetro.updater.NewsChecker;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Version;

import java.util.HashMap;
import java.util.Map;

public abstract class Authenticator {
    public static final Class<? extends Authenticator>[] SUPPORTED_NETWORKS =
            new Class[] {MosMetro.class, AURA.class};

    // Result state
    public static enum RESULT {
        CONNECTED, ALREADY_CONNECTED,
        NOT_REGISTERED, ERROR,
        INTERRUPTED, UNSUPPORTED
    }

    // Network check state
    public static enum CHECK {
        CONNECTED, WRONG_NETWORK, NOT_CONNECTED
    }

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

    public RESULT start() {
        stopped = false;

        logger.log(String.format(
                context.getString(R.string.version), new Version(context).getFormattedVersion()
        ));
        RESULT result = connect();

        if (stopped) return result;

        if (result == RESULT.CONNECTED || result == RESULT.ALREADY_CONNECTED)
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

    public abstract CHECK isConnected();

    protected abstract RESULT connect();

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

    private void submit_info (RESULT result) {
        String STATISTICS_URL = new CachedRetriever(context)
                .get(URLs.STAT_URL_SRC, URLs.STAT_URL_DEF) + URLs.STAT_REL_CHECK;

        Map<String,String> params = new HashMap<String, String>();
        params.put("version", new Version(context).getFormattedVersion());
        params.put("connected", result == RESULT.CONNECTED ? "1" : "0");
        params.put("ssid", getSSID());

        try {
            new OkHttp().post(STATISTICS_URL, params);
        } catch (Exception ignored) {}

        if (settings.getBoolean("pref_notify_news", true))
            new NewsChecker(context).check();
    }
}