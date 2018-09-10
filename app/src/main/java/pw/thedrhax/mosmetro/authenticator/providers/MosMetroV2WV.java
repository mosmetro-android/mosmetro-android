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
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.Nullable;

import org.jsoup.nodes.Element;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.NamedTask;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.authenticator.Task;
import pw.thedrhax.mosmetro.authenticator.WaitTask;
import pw.thedrhax.mosmetro.authenticator.WebViewInterceptorTask;
import pw.thedrhax.mosmetro.authenticator.WebViewProvider;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.ParsedResponse;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.mosmetro.services.WebViewService;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Util;

/**
 * The MosMetroV2VW class implements support for auth.wi-fi.ru algorithm using Android WebView
 * component to create requests and interpret server answers.
 *
 * Detection: Meta-redirect contains ".wi-fi.ru" with any 3rd level domain (except "login"
 * and "welcome").
 *
 * When pref_mosmetro_v3 is disabled, welcome.wi-fi.ru will be handled and bypassed by MosMetroV2WV
 * in all regions except Saint Petersburg (see MosMetroV3 instead).
 *
 * Overrides: MosMetroV2
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see Provider
 */

public class MosMetroV2WV extends WebViewProvider {
    private String redirect = "http://auth.wi-fi.ru/";

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
                    if (redirect.contains("segment")) {
                        vars.put("segment", Uri.parse(redirect).getQueryParameter("segment"));
                    } else {
                        vars.put("segment", "metro");
                    }
                    return true;
                }
            }
        });

        /**
         * Checking for bad redirect
         * redirect ~= welcome.wi-fi.ru
         */
        if (!settings.getBoolean("pref_mosmetro_v3", true))
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                if (redirect.contains("welcome.wi-fi.ru")) {
                    Logger.log(Logger.LEVEL.DEBUG, "Found redirect to welcome.wi-fi.ru!");

                    try {
                        client.get(redirect, null, pref_retry_count);
                        Logger.log(Logger.LEVEL.DEBUG, client.response().getPage());
                    } catch (IOException ex) {
                        Logger.log(Logger.LEVEL.DEBUG, ex);
                    }

                    redirect = Uri.parse(redirect).buildUpon()
                            .authority("auth.wi-fi.ru")
                            .build().toString();

                    vars.put("v3_bypass", "true");
                    Logger.log(Logger.LEVEL.DEBUG, redirect);
                }
                return true;
            }
        });

        /**
         * Async: Block some URL patterns for performance and stability
         */
        String def_pattern = context.getString(R.string.pref_mmv2wv_blacklist_default);
        String pattern = settings.getString("pref_mmv2wv_blacklist", def_pattern);
        if (!def_pattern.equals(pattern)) {
            Logger.log(this, "Warning | Using custom blacklist RegExp: " + pattern);
        }
        add(new WebViewInterceptorTask(pattern) {
            @Nullable @Override
            public ParsedResponse request(WebViewService wv, Client client, String url) {
                Logger.log(Logger.LEVEL.DEBUG, "Blocked: " + url);
                return new ParsedResponse("");
            }
        });

        /**
         * Async: https://auth.wi-fi.ru/auth
         * * Parse CSRF token
         * * Insert automation script into response
         */
        add(new WebViewInterceptorTask("https?://auth\\.wi-fi\\.ru/auth(\\?.*)?") {
            @Nullable @Override
            public ParsedResponse request(WebViewService wv, Client client, String url) throws IOException {
                ParsedResponse response = client.get(url, null, pref_retry_count);

                try {
                    String csrf_token = response.parseMetaContent("csrf-token");
                    Logger.log(Logger.LEVEL.DEBUG, "CSRF token: " + csrf_token);
                    client.setHeader(Client.HEADER_CSRF, csrf_token);
                } catch (ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_server)
                    ));
                }

                Element script = response.getPageContent().body().appendElement("script");
                script.text(Util.readAsset(context, "MosMetroV2.js"));

                return response;
            }
        });

        /**
         * Async: Detect ban (redirect to /auto_auth)
         */
        add(new WebViewInterceptorTask("https?://auth\\.wi-fi\\.ru/auto_auth.*?") {
            @Nullable @Override
            public ParsedResponse request(WebViewService wv, Client client, String url) {
                Logger.log(context.getString(R.string.auth_ban_message));

                // Increase ban counter
                settings.edit()
                        .putInt("metric_ban_count", settings.getInt("metric_ban_count", 0) + 1)
                        .apply();

                context.sendBroadcast(new Intent("pw.thedrhax.mosmetro.event.MosMetroV2.BANNED"));

                running.set(false);
                return new ParsedResponse("");
            }
        });

        /**
         * Async: Replace GET /auth/init with POST /auth/init
         */
        add(new WebViewInterceptorTask("https?://auth\\.wi-fi\\.ru/auth/init(\\?.*)?") {
            @Nullable @Override
            public ParsedResponse request(WebViewService wv, Client client, String url) throws IOException {
                return client.post(url, null, pref_retry_count);
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
                try {
                    wv.get(redirect);
                } catch (Exception ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    return false;
                }
                return true;
            }
        });

        /**
         * Waiting for auth page to load
         */
        add(new WaitTask(this, "Waiting for auth page to load") {
            @Override
            public boolean condition() {
                return !wv.getUrl().contains("auth.wi-fi.ru/auth");
            }
        });

        /**
         * Waiting for WebView to try to load any other URL
         */
        add(new WaitTask(this, "Waiting for script") {
            @Override
            public boolean condition() {
                return wv.getUrl().contains("auth.wi-fi.ru/auth");
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
            redirect = client.response().parseMetaRedirect();
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
    public static boolean match(ParsedResponse response, SharedPreferences settings) {
        if (!settings.getBoolean("pref_webview_enabled", true)) return false;

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
