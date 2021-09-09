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
import androidx.annotation.NonNull;

import org.acra.ACRA;

import java.util.HashMap;
import java.util.LinkedList;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.providers.MAInet;
import pw.thedrhax.mosmetro.authenticator.providers.MosMetroV1;
import pw.thedrhax.mosmetro.authenticator.providers.MosMetroV2;
import pw.thedrhax.mosmetro.authenticator.providers.MosMetroV2WV;
import pw.thedrhax.mosmetro.authenticator.providers.MosMetroV3;
import pw.thedrhax.mosmetro.authenticator.providers.Unknown;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.HttpResponse;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Listener;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Randomizer;

/**
 * Base class for all providers.
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see LinkedList
 * @see Task
 */

public abstract class Provider extends LinkedList<Task> implements Task {
    /**
     * List of supported SSIDs
     */
    public static final String[] SSIDs = {
            "MosMetro_Free",
            "AURA",
            "MosGorTrans_Free",
            "MT_FREE", "MT_FREE_",
            "CPPK_Free",
            "Air_WiFi_Free",
            "MAInet_public",
            "Moscow_WiFi_Free"
            "bmstu_lb"
    };

    protected Context context;
    protected SharedPreferences settings;
    protected Randomizer random;
    protected Gen204 gen_204;
    private boolean nested = false;

    /**
     * Default Client used for all network operations
     */
    protected Client client;

    /**
     * Find Provider using already received response from server.
     * @param context   Android Context required to create the new instance.
     * @param response  Instance of ParsedResponse.
     * @return          New Provider instance.
     *
     * @see Client
     */
    @NonNull public static Provider find(Context context, HttpResponse response) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        if (MosMetroV3.match(response, settings)) return new MosMetroV3(context, response);
        else if (MosMetroV2WV.match(response, settings)) return new MosMetroV2WV(context, response);
        else if (MosMetroV2.match(response)) return new MosMetroV2(context, response);
        else if (MosMetroV1.match(response)) return new MosMetroV1(context, response);
        else if (MAInet.match(response) && settings.getBoolean("pref_mainet", true)) return new MAInet(context, response);
        else if (BMSTU_lb.match(Response) && settings.getBoolean("pref_bmstu_lb", true)) return new BMSTU_lb(context, response);
        else return new Unknown(context, response);
    }

    /**
     * Find Provider by sending predefined request to get the redirect.
     *
     * @param context   Android Context required to create the new instance.
     * @param running   Listener used to interrupt this method.
     * @return          New Provider instance.
     */
    @NonNull public static Provider find(Context context, Listener<Boolean> running) {
        Logger.log(context.getString(R.string.auth_provider_check));
        HttpResponse response = new Gen204(context, running).check().getResponse();
        Provider result = Provider.find(context, response);
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
        this.random = new Randomizer(context);
        this.gen_204 = new Gen204(context, running);
        setClient(new OkHttp(context));
    }

    /**
     * Checks network connection state for a specific provider.
     * This method ignores false negatives by default.
     * 
     * @return True if internet access is available; otherwise, false is returned.
     */
    public boolean isConnected() {
        return isConnected(gen_204.check().getResponse());
    }

    /**
     * Checks ParsedResponse to be a valid generate_204 response.
     * @return True if generate_204 response is valid; otherwise, false is returned.
     */
    protected static boolean isConnected(HttpResponse response) {
        return response.getResponseCode() == 204;
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
        INTERRUPTED, RESTART,                   // Stopped
    }

    /**
     * Initialize this Provider and it's children.
     * @return true on success, false on error
     */
    protected boolean init() {
        for (Task task : this) {
            if (task instanceof InterceptorTask && !client.interceptors.contains(task)) {
                client.interceptors.add((InterceptorTask) task);
            }
        }

        return true;
    }

    /**
     * Reverse effect of init().
     */
    protected void deinit() {
        for (Task task : this) {
            if (task instanceof InterceptorTask) {
                client.interceptors.remove(task);
            }
        }
    }

    /**
     * Start the connection sequence defined in child classes.
     */
    public RESULT start(HashMap<String,Object> vars) {
        ProviderMetrics metrics = new ProviderMetrics(this);

        if (!nested) {
            metrics.start();
            vars.put("result", RESULT.ERROR);
            Logger.date(">> ");
        }

        if (!init()) {
            Logger.log(this, "Initialization failed");
            return RESULT.ERROR;
        }

        for (int i = 0; i < size(); i++) {
            if (isStopped()) {
                if (vars.get("result") == RESULT.ERROR)
                    vars.put("result", RESULT.INTERRUPTED);
                break;
            }

            final int progress = (i + 1) * 100 / size();
            Task task = get(i);

            if (task instanceof Provider) {
                Provider nested = (Provider) task;

                nested.setNested(true);
                nested.setClient(client);
                nested.setRunningListener(running);
                nested.setCallback(new ICallback() {
                    @Override
                    public void onProgressUpdate(int nested_progress) {
                        callback.onProgressUpdate(progress + nested_progress / size());
                    }

                    @Override
                    public void onProgressUpdate(int nested_progress, String message) {
                        callback.onProgressUpdate(progress + nested_progress / size(), message);
                    }
                });

                Logger.log(context.getString(R.string.auth_algorithm_switch, nested.getName()));
                vars.put("switch", nested.getName());

                nested.start(vars);
                continue;
            }

            if (nested && task instanceof FinalConnectionCheckTask) continue;

            if (task instanceof NamedTask) {
                Logger.log(((NamedTask) task).getName());
                callback.onProgressUpdate(progress, ((NamedTask) task).getName());
            } else {
                callback.onProgressUpdate(progress);
            }

            try {
                if (!task.run(vars)) break;
            } catch (RuntimeException ex) {
                Logger.log(Logger.LEVEL.DEBUG, ex);
                Logger.log(context.getString(R.string.error,
                        context.getString(R.string.auth_error_fatal)
                ));
                ACRA.getErrorReporter().handleSilentException(ex);
                break;
            }
        }

        if (!nested) metrics.end(vars);

        deinit();

        if (!nested) Logger.date("<< ");

        return (RESULT)vars.get("result");
    }

    public RESULT start() {
        return start(new HashMap<>());
    }

    @Override
    public boolean run(HashMap<String, Object> vars) {
        throw new RuntimeException("Provider is a special type of Task");
    }

    public Provider setNested(boolean nested) {
        this.nested = nested;
        return this;
    }

    public boolean isNested() {
        return nested;
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
     * Replace default Client
     */
    public Provider setClient(Client client) {
        this.client = client
                .setRunningListener(running)
                .setDelaysEnabled(settings.getBoolean("pref_delay_enabled", false));
        return this;
    }

    public Client getClient() {
        return client;
    }

    /**
     * Replace default Gen204 provider
     */
    public Provider setGen204(Gen204 gen_204) {
        this.gen_204 = gen_204;
        return this;
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

        /**
         * Report the progress of algorithm execution with the description of current Task.
         * @param progress  Any Integer between 0 and 100.
         * @param message   Text massage to display in notification.
         */
        void onProgressUpdate(int progress, String message);
    }

    protected ICallback callback = new ICallback() {
        @Override
        public void onProgressUpdate(int progress) {

        }

        @Override
        public void onProgressUpdate(int progress, String message) {

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
