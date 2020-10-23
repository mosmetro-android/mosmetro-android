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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jsoup.nodes.Element;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.InitialConnectionCheckTask;
import pw.thedrhax.mosmetro.authenticator.InterceptorTask;
import pw.thedrhax.mosmetro.authenticator.NamedTask;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.authenticator.Task;
import pw.thedrhax.mosmetro.authenticator.WaitTask;
import pw.thedrhax.mosmetro.authenticator.WebViewProvider;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.ParsedResponse;
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
    private String redirect = "http://auth.wi-fi.ru/?segment=metro";

    /**
     * Saint-Petersburg branch mode. Replaces hard-coded URLs.
     *
     * auth.wi-fi.ru → auth.wi-fi.ru/spb/new
     * auth.wi-fi.ru/auth → auth.wi-fi.ru/spb/gapi/auth/start
     * auth.wi-fi.ru/auth/init → auth.wi-fi.ru/spb/gapi/auth/init
     * auth.wi-fi.ru/auth/check → auth.wi-fi.ru/spb/gapi/auth/check
     * auth.wi-fi.ru/identification → auth.wi-fi.ru/spb/identification
     */
    private Boolean spb = false;

    public MosMetroV2WV(Context context, ParsedResponse res) {
        super(context);

        /**
         * Checking Internet connection for a first time
         * ⇒ GET generate_204 < res
         * ⇐ Meta-redirect: http://auth.wi-fi.ru/?segment=... > redirect, segment
         */
        add(new InitialConnectionCheckTask(this, res) {
            @Override
            public boolean handle_response(HashMap<String, Object> vars, ParsedResponse response) {
                try {
                    redirect = response.parseAnyRedirect();
                } catch (ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(Logger.LEVEL.DEBUG, "Redirect not found in response, using default");
                }

                Logger.log(Logger.LEVEL.DEBUG, redirect);

                Uri uri = Uri.parse(redirect);

                if (uri.getPath().startsWith("/spb/new")) {
                    Logger.log(Logger.LEVEL.DEBUG, "Saint-Petersburg branch detected. Replacing URLs");
                    spb = true;
                }

                if (uri.getQueryParameter("segment") != null) {
                    vars.put("segment", uri.getQueryParameter("segment"));
                } else {
                    vars.put("segment", "metro");
                }

                return true;
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
                        ParsedResponse response = client.get(redirect, null, pref_retry_count);
                        Logger.log(Logger.LEVEL.DEBUG, response.getPage());
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
        add(new InterceptorTask(this, pattern) {
            @Nullable @Override
            public ParsedResponse request(Client client, Client.METHOD method, String url, Map<String, String> params) throws IOException {
                Logger.log(Logger.LEVEL.DEBUG, "Blocked: " + url);
                return new ParsedResponse("");
            }
        });

        /**
         * Async: https://auth.wi-fi.ru/auth
         * - Detect ban (302 redirect to /auto_auth)
         * - Detect if device is not registered in the network (302 redirect to /identification)
         * - Parse CSRF token
         * - Insert automation script into response
         */
        add(new InterceptorTask(this, "https?://auth\\.wi-fi\\.ru/(auth|spb/gapi/auth/start)(\\?.*)?") {
            @Nullable @Override
            public ParsedResponse request(Client client, Client.METHOD method, String url, Map<String, String> params) throws IOException {
                client.followRedirects(false);
                ParsedResponse response = client.get(url, null, pref_retry_count);
                Logger.log(Logger.LEVEL.DEBUG, response.toString());
                client.followRedirects(true);
                return response;
            }

            @NonNull @Override
            public ParsedResponse response(Client client, String url, ParsedResponse response) throws IOException {
                try {
                    String redirect = response.get300Redirect();

                    if (redirect.contains("auto_auth")) {
                        Logger.log(context.getString(R.string.auth_ban_message));

                        // Increase ban counter
                        settings.edit()
                                .putInt("metric_ban_count", settings.getInt("metric_ban_count", 0) + 1)
                                .apply();

                        context.sendBroadcast(new Intent("pw.thedrhax.mosmetro.event.MosMetroV2.BANNED"));

                        running.set(false);
                        return new ParsedResponse("");
                    }

                    if (redirect.contains("/identification")) { // not registered
                        Logger.log(context.getString(R.string.error,
                                context.getString(R.string.auth_error_not_registered)
                        ));

                        vars.put("result", RESULT.NOT_REGISTERED);
                        running.set(false);
                        return new ParsedResponse("");
                    }
                } catch (ParseException ignored) {}

                try {
                    String csrf_token = response.parseMetaContent("csrf-token");
                    Logger.log(Logger.LEVEL.DEBUG, "CSRF token: " + csrf_token);
                    client.setHeader(Client.HEADER_CSRF, csrf_token);
                } catch (ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, "CSRF token not found");
                }

                String mosmetro_js = Util.readAsset(context, "MosMetroV2.js");
                mosmetro_js = "<script>" + mosmetro_js + "</script>";
                response.getPageContent().body().append(mosmetro_js);

                return response;
            }
        });

        /**
         * Async: Replace GET /auth/init with POST /auth/init
         */
        add(new InterceptorTask(this, "https?://auth\\.wi-fi\\.ru/(spb/gapi/)?auth/init(\\?.*)?") {
            @Nullable @Override
            public ParsedResponse request(Client client, Client.METHOD method, String url, Map<String, String> params) throws IOException {
                return client.post(url, null, pref_retry_count);
            }
        });

        /**
         * Async: Block loading of https://(spb.)wi-fi.ru but send request anyway
         */
        add(new InterceptorTask(this, "https?://(spb.)?wi-fi\\.ru/.*") {
            @NonNull @Override
            public ParsedResponse response(Client client, String url, ParsedResponse response) throws IOException {
                return new ParsedResponse("");
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
                wv.get(redirect);
                return true;
            }
        });

        /**
         * Waiting for auth page to load
         */
        add(new WaitTask(this, "Waiting for auth page to load") {
            @Override
            public boolean until(HashMap<String, Object> vars) {
                if (!spb) {
                    return wv.getURL().contains("auth.wi-fi.ru/auth");
                } else {
                    return wv.getURL().contains("auth.wi-fi.ru/spb/gapi/auth/start");
                }
            }
        }.timeout(60000));

        /**
         * Waiting for WebView to try to load any other URL
         * Also check internet connection once every internet_check_interval (10 seconds by default)
         */
        add(new WaitTask(this, "Waiting for script") {
            private final boolean pref_internet_check = settings.getBoolean("pref_internet_check", true);
            private final int interval = Util.getIntPreference(context, "pref_internet_check_interval", 10);
            private int counter = 0;

            @Override
            public boolean until(HashMap<String, Object> vars) {
                if (pref_internet_check && ++counter == interval * 10) {
                    counter = 0;
                    return isConnected();
                }

                if (!spb) {
                    return !wv.getURL().contains("auth.wi-fi.ru/auth");
                } else {
                    return !wv.getURL().contains("auth.wi-fi.ru/spb/gapi/auth/start");
                }
            }
        }.timeout(120000));

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

    /**
     * Checks if current network is supported by this Provider implementation.
     * @param response  Instance of ParsedResponse.
     * @return          True if response matches this Provider implementation.
     */
    public static boolean match(ParsedResponse response, SharedPreferences settings) {
        if (!settings.getBoolean("pref_mosmetro_v2wv", true)) return false;

        String redirect;

        try {
            redirect = response.parseAnyRedirect();
        } catch (ParseException ex1) {
            return false;
        }

        return redirect.contains(".wi-fi.ru") && !redirect.contains("login.wi-fi.ru");
    }
}
