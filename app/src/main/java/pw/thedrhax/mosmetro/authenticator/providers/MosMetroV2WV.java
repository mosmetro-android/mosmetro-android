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

package pw.thedrhax.mosmetro.authenticator.providers;

import android.content.Context;
import android.os.SystemClock;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.NamedTask;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.authenticator.WebViewProvider;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.ParsedResponse;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Util;

/**
 * The MosMetroV2VW class implements support for auth.wi-fi.ru and welcome.wi-fi.ru algorithms
 * using Android WebView component to create requests.
 *
 * Detection: Meta-redirect contains ".wi-fi.ru" with any 3rd level domain (except "login").
 *
 * Overrides: MosMetroV2, MosMetroV3
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see Provider
 */

public class MosMetroV2WV extends WebViewProvider {

    public MosMetroV2WV(Context context) {
        super(context);

        /**
         * Checking Internet connection for a first time
         * ⇒ GET generate_204
         * ⇐ Meta-redirect: http://auth.wi-fi.ru/?segment=... > redirect, segment
         */
        add(new NamedTask(context.getString(R.string.auth_checking_connection)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                if (isConnected()) {
                    Logger.log(context.getString(R.string.auth_already_connected));
                    vars.put("result", RESULT.ALREADY_CONNECTED);
                    return false;
                } else {
                    return true;
                }
            }
        });

        /**
         * Opening auth page
         * ⇒ GET https://auth.wi-fi.ru
         * ⇐ JavaScript redirect: /auth
         * ⇒ GET /auth
         * ⇐ 200 OK
         */
        add(new NamedTask("Opening auth page") {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                wv.setBlacklist(new String[]{"ads.adfox.ru", "mc.yandex.ru", "ac.yandex.ru", ".mp4"});
                try {
                    wv.get("https://auth.wi-fi.ru/");
                } catch (Exception ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    return false;
                }
                return true;
            }
        });

        /**
         * Loading automated login script from assets
         */
        add(new NamedTask("Loading automated login script") {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                try {
                    String script = Util.readAsset(context, "MosMetroV2.js");
                    String result = wv.js(script);
                    Logger.log(Logger.LEVEL.DEBUG, result);

                    if ("MosMetroV2.js loaded".equals(result)) {
                        Logger.log("Script loaded successfully!");
                    } else {
                        Logger.log(context.getString(R.string.error,
                                "Script didn't load correctly"
                        ));
                        return false;
                    }
                } catch (Exception ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            "Couldn't load the script"
                    ));
                    return false;
                }

                return true;
            }
        });

        /**
         * Waiting for WebView to try to load any other URL
         *
         * ⇒ GET https://wi-fi.ru
         * ⇐ Don't wait for response
         */
        add(new NamedTask("Waiting for auth page to close") {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                while (wv.getCurrentUrl().contains("auth.wi-fi.ru/auth")) {
                    SystemClock.sleep(100);
                    if (!running.get()) {
                        return false;
                    }
                }
                return true;
            }
        });

        /**
         * Checking Internet connection
         */
        add(new NamedTask(context.getString(R.string.auth_checking_connection)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                if (isConnected()) {
                    Logger.log(context.getString(R.string.auth_connected));
                    vars.put("result", RESULT.CONNECTED);
                    return true;
                } else {
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_connection)
                    ));
                    return false;
                }
            }
        });
    }

    @Override
    public boolean isConnected() {
        Client client = new OkHttp(context).followRedirects(false);
        try {
            client.get("http://" + random.choose(GENERATE_204), null, pref_retry_count);
        } catch (IOException ex) {
            Logger.log(Logger.LEVEL.DEBUG, ex);
            return false;
        }

        try {
            String redirect = client.response().parseMetaRedirect();
            Logger.log(Logger.LEVEL.DEBUG, redirect);
        } catch (ParseException ex) {
            // Redirect not found => connected
            return super.isConnected();
        }

        // Redirect found => not connected
        return false;
    }

    /**
     * Checks if current network is supported by this Provider implementation.
     * @param response  Instance of ParsedResponse.
     * @return          True if response matches this Provider implementation.
     */
    public static boolean match(ParsedResponse response) {
        String redirect;

        try {
            redirect = response.parseMetaRedirect();
        } catch (ParseException ex1) {
            try {
                redirect = response.get300Redirect();
            } catch (ParseException ex2) {
                return false;
            }
        }

        return redirect.contains(".wi-fi.ru") && !redirect.contains("login.wi-fi.ru");
    }
}
