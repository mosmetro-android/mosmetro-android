/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 * Copyright © 2015 Dmitry Karikh <the.dr.hax@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pw.thedrhax.mosmetro.authenticator;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pw.thedrhax.mosmetro.BuildConfig;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.providers.Enforta;
import pw.thedrhax.mosmetro.authenticator.providers.MosMetroV1;
import pw.thedrhax.mosmetro.authenticator.providers.MosMetroV2;
import pw.thedrhax.mosmetro.authenticator.providers.Unknown;
import pw.thedrhax.mosmetro.httpclient.CachedRetriever;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.mosmetro.updater.NewsChecker;
import pw.thedrhax.util.AndroidHacks;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Util;
import pw.thedrhax.util.Version;
import pw.thedrhax.util.WifiUtils;

/**
 * Base class for all providers.
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see LinkedList
 * @see Task
 * @see pw.thedrhax.util.Logger.ILogger
 */

public abstract class Provider extends LinkedList<Task> implements Logger.ILogger<Provider> {
    /**
     * List of supported Providers
     *
     * Every Provider in this list must implement the
     *  "public static boolean match(Client)"
     * method to be detected by Provider.find().
     * @see MosMetroV1
     */
    private static final List<Class<? extends Provider>> PROVIDERS =
            new LinkedList<Class<? extends Provider>>() {{
                add(MosMetroV1.class);
                add(MosMetroV2.class);
                add(Enforta.class);
            }};

    /**
     * List of supported SSIDs
     */
    public static final String[] SSIDs = {
            "MosMetro_Free",
            "AURA",
            "MosGorTrans_Free",
            "MT_FREE",
            "Air_WiFi_Free"
    };

    protected Context context;
    protected SharedPreferences settings;

    /**
     * Number of retries for each request
     */
    protected int pref_retry_count;

    /**
     * Default Client used for all network operations
     */
    protected Client client;

    /**
     * Find Provider using already received response from server.
     * @param context   Android Context required to create the new instance.
     * @param client    Client, that contains server's response.
     * @return          New Provider instance.
     *
     * @see Client
     */
    @NonNull public static Provider find(Context context, Client client) {
        for (Class<? extends Provider> provider_class : PROVIDERS) {
            try {
                if ((Boolean)provider_class
                        .getMethod("match", Client.class)
                        .invoke(null, client))
                    return provider_class
                            .getConstructor(Context.class)
                            .newInstance(context);
            } catch (Exception ignored) {}
        }
        return new Unknown(context);
    }

    /**
     * Find Provider by sending predefined request to "wi-fi.ru" to get the redirect.
     * @param context   Android Context required to create the new instance.
     * @param logger    Logger to get debug messages from this method and the resulting Provider.
     * @return          New Provider instance.
     */
    @NonNull public static Provider find(Context context, Logger logger) {
        Client client = new OkHttp().followRedirects(false);
        int pref_retry_count = Util.getIntPreference(context, "pref_retry_count", 3);
        try {
            client.get("http://wi-fi.ru", null, pref_retry_count);
        } catch (Exception ex) {
            logger.log(Logger.LEVEL.DEBUG, ex);
            logger.log(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_redirect)
            ));
            return new Unknown(context).setLogger(logger);
        }

        // If Provider is Unknown, write response body to log
        Provider provider = find(context, client).setLogger(logger);
        if (provider instanceof Unknown) {
            logger.log(Logger.LEVEL.DEBUG, client.getPageContent().toString());
        }
        return provider;
    }

    /**
     * Check if a particular SSID is supported.
     * @param SSID  SSID of the Wi-Fi network to be tested.
     * @return  True if network is supported; otherwise, false.
     */
    public static boolean isSSIDSupported(String SSID) {
        for (String a : SSIDs) {
            if (a.equals(SSID))
                return true;
        }
        // TODO: Issue #70
        return false;
    }

    /**
     * Main constructor
     * @param context   Android application Context
     */
    public Provider(Context context) {
        this.context = context;
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);
        this.pref_retry_count = Util.getIntPreference(context, "pref_retry_count", 3);
        this.client = new OkHttp();
    }

    /**
     * Checks network connection state without binding to a specific provider.
     * This implementation uses generate_204 method, that is default for Android.
     * @return True if internet access is available; otherwise, false is returned.
     */
    public static boolean generate_204() {
        Client client = new OkHttp();
        try {
            client.get("http://google.ru/generate_204", null);
        } catch (Exception ex) {
            if (client.getResponseCode() == 204)
                return true;
        }
        return false;
    }

    /**
     * Checks network connection state for a specific provider.
     * @return True if internet access is available; otherwise, false is returned.
     */
    public boolean isConnected() {
        return generate_204();
    }

    /**
     * Get Provider's short description.
     * @return Provider's name.
     */
    public String getName() {
        return this.getClass().getSimpleName();
    }

    public enum RESULT {
        CONNECTED, ALREADY_CONNECTED,           // Success
        CAPTCHA,                                // User action required
        NOT_REGISTERED, ERROR, NOT_SUPPORTED,   // Error
        INTERRUPTED                             // Stopped
    }

    /**
     * Start the connection sequence defined in child classes.
     */
    public RESULT start() {
        AndroidHacks.bindToWiFi(context);

        logger.log(String.format(
                context.getString(R.string.version), Version.getFormattedVersion()
        ));
        logger.log(String.format(context.getString(R.string.algorithm_name), getName()));

        HashMap<String,Object> vars = new HashMap<>();
        vars.put("result", RESULT.ERROR);
        for (Task task : this) {
            if (stopped) return RESULT.INTERRUPTED;
            callback.onProgressUpdate((indexOf(task) + 1) * 100 / size());
            if (!task.run(vars)) break;
        }

        /**
         * Submit statistics
         */
        new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                boolean connected;

                switch ((RESULT) vars.get("result")) {
                    case CONNECTED: connected = true; break;
                    case ALREADY_CONNECTED: connected = false; break;
                    default: return false;
                }

                String STATISTICS_URL = new CachedRetriever(context).get(
                        BuildConfig.API_URL_SOURCE, BuildConfig.API_URL_DEFAULT
                ) + BuildConfig.API_REL_STATISTICS;

                Map<String,String> params = new HashMap<>();
                params.put("version", Version.getFormattedVersion());
                params.put("success", connected ? "true" : "false");
                params.put("ssid", new WifiUtils(context).getSSID());
                params.put("provider", getName());
                if (vars.get("captcha") != null)
                    params.put("captcha", (String) vars.get("captcha"));

                try {
                    new OkHttp().post(STATISTICS_URL, params);
                } catch (Exception ignored) {}

                if (settings.getBoolean("pref_notify_news", true))
                    new NewsChecker(context).check();

                return false;
            }
        }.run(vars);

        return (RESULT)vars.get("result");
    }

    /**
     * This variable is being checked before every Task to be executed.
     */
    protected boolean stopped = false;

    /**
     * Stop the connection sequence from another thread
     * @return Saved instance of this Provider.
     */
    public Provider stop() {
        stopped = true; return this;
    }

    /**
     * The ICallback interface is used by other classes to get messages from Provider
     * during runtime.
     */
    public interface ICallback {
        /**
         * Report the progress of algorithm execution.
         * @param progress  Any Integer between 0 and 100.
         */
        void onProgressUpdate(int progress);
    }

    protected ICallback callback = new ICallback() {
        @Override
        public void onProgressUpdate(int progress) {

        }
    };

    /**
     * Set callback for this Provider.
     * @param callback  Any implementation of the ICallback interface.
     * @return  Saved instance of this Provider.
     */
    public Provider setCallback(ICallback callback) {
        this.callback = callback; return this;
    }

    /*
     * Logger.ILogger implementation
     */

    protected Logger logger = new Logger();

    @Override
    public Provider setLogger(Logger logger) {
        this.logger = logger; return this;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}
