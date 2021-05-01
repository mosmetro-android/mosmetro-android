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
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.regex.Pattern;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.InitialConnectionCheckTask;
import pw.thedrhax.mosmetro.authenticator.InterceptorTask;
import pw.thedrhax.mosmetro.authenticator.NamedTask;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.authenticator.Task;
import pw.thedrhax.mosmetro.authenticator.WaitTask;
import pw.thedrhax.mosmetro.authenticator.WebViewProvider;
import pw.thedrhax.mosmetro.authenticator.Gen204.Gen204Result;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.Headers;
import pw.thedrhax.mosmetro.httpclient.HttpRequest;
import pw.thedrhax.mosmetro.httpclient.HttpResponse;
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

    public MosMetroV2WV(Context context, HttpResponse res) {
        super(context);

        /**
         * Checking Internet connection for a first time
         * ⇒ GET generate_204 < res
         * ⇐ Meta-redirect: http://auth.wi-fi.ru/?segment=... > redirect, segment
         */
        add(new InitialConnectionCheckTask(this, res) {
            @Override
            public boolean handle_response(HashMap<String, Object> vars, HttpResponse response) {
                try {
                    redirect = response.parseAnyRedirect();
                } catch (ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(Logger.LEVEL.DEBUG, "Redirect not found in response, using default");
                }

                Logger.log(Logger.LEVEL.DEBUG, redirect);

                Uri uri = Uri.parse(redirect);
                String path = uri.getPath();

                if (path.startsWith("/auth")) {
                    vars.put("branch", "default");
                } else if (path.startsWith("/spb")) {
                    vars.put("branch", "spb");
                } else if (path.isEmpty() || path.equals("/") || path.startsWith("/new")) {
                    String dn = uri.getQueryParameter("dn");
                    boolean ruckus = dn != null && dn.contains("ruckus");

                    vars.put("branch", ruckus ? "metro-ruckus" : "metro");
                } else {
                    vars.put("branch", "unknown");
                    Logger.log(Logger.LEVEL.DEBUG, "Warning: Unknown path" + path);
                }

                Logger.log(Logger.LEVEL.DEBUG, "Branch: " + vars.get("branch"));

                if (uri.getQueryParameter("segment") != null) {
                    vars.put("segment", uri.getQueryParameter("segment"));
                } else {
                    vars.put("segment", "metro");
                }

                Logger.log(Logger.LEVEL.DEBUG, "Segment: " + vars.get("segment"));

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
                        HttpResponse response = client.get(redirect).setTries(pref_retry_count).execute();
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
        add(new InterceptorTask(".*(ads\\.adfox\\.ru|mc\\.yandex\\.ru|ac\\.yandex\\.ru|\\.mp4$).*") {
            @Nullable @Override
            public HttpResponse request(Client client, HttpRequest request) throws IOException {
                Logger.log(Logger.LEVEL.DEBUG, "Blocked: " + request.getUrl());
                return new HttpResponse(request, "");
            }
        });

        String key = random.string(25).toLowerCase();

        /**
         * Async: Fake response with MosMetroV2.js script
         */
        add(new InterceptorTask("https://" + key + "/MosMetroV2\\.js") {
            @Override
            public HttpResponse request(Client client, HttpRequest request) throws IOException {
                return new HttpResponse(
                        request,
                        Util.readAsset(context, "MosMetroV2.js"),
                        "text/javascript"
                );
            }
        });

        final Pattern auth_page = Pattern.compile("https?://auth\\.wi-fi\\.ru/(auth|spb|new)?/?(\\?.*)?");

        /**
         * Async: https://auth.wi-fi.ru/auth
         *        https://auth.wi-fi.ru/
         *        https://auth.wi-fi.ru/new/
         *        https://auth.wi-fi.ru/spb/
         * - Parse CSRF token
         * - Insert automation script into response
         */
        add(new InterceptorTask(auth_page) {
            @Nullable @Override
            public HttpResponse request(Client client, HttpRequest request) throws IOException {
                client.setFollowRedirects(false);
                HttpResponse response = request.setTries(pref_retry_count).execute();
                client.setFollowRedirects(true);
                return response;
            }

            @NonNull @Override
            public HttpResponse response(Client client, HttpRequest request, HttpResponse response) throws IOException {
                try {
                    String redirect = response.get300Redirect();

                    // Follow 3xx redirect because it can not be passed to WebView
                    response = client.get(redirect).setTries(pref_retry_count).execute();
                } catch (ParseException ignored) {}

                try {
                    String csrf_token = response.parseMetaContent("csrf-token");
                    Logger.log(Logger.LEVEL.DEBUG, "CSRF token: " + csrf_token);
                    client.headers.setHeader(Headers.CSRF, csrf_token);
                } catch (ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, "CSRF token not found");
                }

                response.getPageContent().body().append("<script src=\"https://" + key + "/MosMetroV2.js\"></script>");
                return response;
            }
        });

        /**
         * Async: https://auth.wi-fi.ru/identification
         *        https://auth.wi-fi.ru/spb/identification
         * - Detect if device is not registered in the network
         */
        add(new InterceptorTask(Pattern.compile("https://auth.wi-fi.ru(/spb)?/identification")) {
            @Override
            public HttpResponse request(Client client, HttpRequest request) throws IOException {
                Logger.log(context.getString(R.string.error,
                        context.getString(R.string.auth_error_not_registered)
                ));

                vars.put("result", RESULT.NOT_REGISTERED);
                running.set(false);
                return new HttpResponse(request, "");
            }
        });

        /**
         * Async: Replace /auth/init_smart with /auth/init
         * https://auth.wi-fi.ru/auth/init_smart
         * https://auth.wi-fi.ru/gapi/auth/init_smart
         * https://auth.wi-fi.ru/spb/gapi/auth/init_smart
         */
        add(new InterceptorTask("https?://auth\\.wi-fi\\.ru/((spb/)?gapi/)?auth/init_smart(\\?.*)?") {
            @Nullable @Override
            public HttpResponse request(Client client, HttpRequest request) throws IOException {
                Logger.log(Logger.LEVEL.DEBUG, "Replacing \"init_smart\" with \"init\"");
                request.setUrl(request.getUrl().replace("auth/init_smart", "auth/init"));
                return request.execute();
            }
        });

        /**
         * Async: Block {mcc,spb}.wi-fi.ru, gowifi.ru
         */
        add(new InterceptorTask("https?://((mcc|spb)\\.wi-fi\\.ru|gowifi\\.ru)/") {
            @Override @NonNull
            public HttpResponse response(Client client, HttpRequest request, HttpResponse response) throws IOException {
                return new HttpResponse(request, "");
            }
        });

        /**
         * Opening auth page
         * ⇒ GET https://auth.wi-fi.ru
         * ⇐ JavaScript redirect: /auth
         * ⇒ GET /auth
         * ⇐ 200 OK
         */
        add(new NamedTask(context.getString(R.string.auth_webview_page)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                wv.get(redirect);
                return true;
            }
        });

        /**
         * Waiting for auth page to load
         */
        add(new WaitTask(this, context.getString(R.string.auth_webview_page_wait)) {
            @Override
            public boolean until(HashMap<String, Object> vars) {
                return auth_page.matcher(wv.getUrl()).matches();
            }
        }.timeout(60000));

        /**
         * Waiting for WebView to try to load any other URL
         * Also check internet connection once every internet_check_interval (10 seconds by default)
         */
        add(new WaitTask(this, context.getString(R.string.auth_webview_script)) {
            private final boolean pref_internet_check = settings.getBoolean("pref_internet_check", true);
            private final int interval = Util.getIntPreference(context, "pref_internet_check_interval", 10);
            private int counter = 0;

            @Override
            public boolean until(HashMap<String, Object> vars) {
                if (pref_internet_check && ++counter == interval * 10) {
                    counter = 0;
                    Gen204Result res_204 = gen_204.check();
                    return res_204.isConnected() && !res_204.isFalseNegative();
                }

                return !auth_page.matcher(wv.getUrl()).matches();
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
    public static boolean match(HttpResponse response, SharedPreferences settings) {
        if (!settings.getBoolean("pref_mosmetro_v2_wv", false)) return false;

        String redirect;

        try {
            redirect = response.parseAnyRedirect();
        } catch (ParseException ex1) {
            return false;
        }

        return redirect.contains(".wi-fi.ru") && !redirect.contains("login.wi-fi.ru");
    }
}
