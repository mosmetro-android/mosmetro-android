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

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.providers.Enforta;
import pw.thedrhax.mosmetro.authenticator.providers.MosMetroV1;
import pw.thedrhax.mosmetro.authenticator.providers.MosMetroV2;
import pw.thedrhax.mosmetro.authenticator.providers.Unknown;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.AndroidHacks;
import pw.thedrhax.util.Listener;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Util;

/**
 * Base class for all providers.
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see LinkedList
 * @see Task
 */

public abstract class Provider extends LinkedList<Task> {
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
            "MT_FREE", "MT_FREE_",
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
     *
     * The first request executed right after connecting to Wi-Fi is known to have
     * direct access to the Internet. This *bug* of the network doesn't allow us to
     * detect the Provider too quickly. This is why we use a retry loop in this method.
     *
     * @param context   Android Context required to create the new instance.
     * @param running   Listener used to interrupt this method.
     * @return          New Provider instance.
     */
    @NonNull public static Provider find(Context context, Listener<Boolean> running) {
        Logger.log(context.getString(R.string.auth_provider_check));

        Client client = new OkHttp(context).followRedirects(false).setRunningListener(running);
        int pref_retry_count = Util.getIntPreference(context, "pref_retry_count", 3);
        try {
            client.get("http://wi-fi.ru", null, pref_retry_count);
        } catch (Exception ex) {
            Logger.log(Logger.LEVEL.DEBUG, ex);
            Logger.log(context.getString(R.string.error,
                    context.getString(R.string.auth_error_redirect)
            ));
        }

        Provider result = Provider.find(context, client);

        if (result instanceof Unknown && !generate_204()) {
            Logger.log(Logger.LEVEL.DEBUG, client.getPage());
            Logger.log(context.getString(R.string.error,
                    context.getString(R.string.auth_error_provider)
            ));
            Logger.log(context.getString(R.string.auth_provider_assume));
            return new MosMetroV2(context);
        }

        return result;
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
        this.client = new OkHttp(context).setRunningListener(running);
    }

    /**
     * Checks network connection state without binding to a specific provider.
     * This implementation uses generate_204 method, that is default for Android.
     * @return True if internet access is available; otherwise, false is returned.
     */
    public static boolean generate_204() {
        Client client = new OkHttp().setTimeout(3000);
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
        NOT_REGISTERED, ERROR, NOT_SUPPORTED,   // Error
        INTERRUPTED                             // Stopped
    }

    /**
     * Start the connection sequence defined in child classes.
     */
    public RESULT start() {
        AndroidHacks.bindToWiFi(context);

        HashMap<String,Object> vars = new HashMap<>();
        vars.put("result", RESULT.ERROR);
        for (Task task : this) {
            if (isStopped()) return RESULT.INTERRUPTED;
            callback.onProgressUpdate((indexOf(task) + 1) * 100 / size());
            if (!task.run(vars)) break;
        }

        new StatisticsTask(this).run(vars);
        return (RESULT)vars.get("result");
    }

    /**
     * Listener used to stop Provider immediately after
     * variable is changed by another thread
     */
    protected final Listener<Boolean> running = new Listener<>(true);

    /**
     * Subscribe to another Listener to implement cascade notifications
     */
    public Provider setRunningListener(Listener<Boolean> master) {
        running.subscribe(master); return this;
    }

    /**
     * Method used to check if Provider must finish as soon as possible.
     * @return true is Provider must stop, otherwise false.
     */
    protected boolean isStopped() {
        return !running.get();
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
}
