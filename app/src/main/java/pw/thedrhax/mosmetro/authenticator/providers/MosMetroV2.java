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
import android.net.Uri;
import androidx.annotation.NonNull;
import android.util.Patterns;

import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.ProtocolException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.InitialConnectionCheckTask;
import pw.thedrhax.mosmetro.authenticator.InterceptorTask;
import pw.thedrhax.mosmetro.authenticator.NamedTask;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.authenticator.Task;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.ParsedResponse;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Randomizer;

/**
 * The MosMetroV2 class implements support for auth.wi-fi.ru algorithm.
 *
 * Detection: Meta-redirect contains ".wi-fi.ru" with any 3rd level domain (except "login").
 *
 * When pref_mosmetro_v3 is disabled, welcome.wi-fi.ru will be handled and bypassed by MosMetroV2
 * in all regions except Saint Petersburg (see MosMetroV3 instead).
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see Provider
 */

public class MosMetroV2 extends Provider {
    private String redirect = "http://auth.wi-fi.ru/?segment=metro";

    // TODO: Split branches into sub-providers

    /**
     * Saint-Petersburg branch
     *
     * auth.wi-fi.ru → none
     * auth.wi-fi.ru/auth → auth.wi-fi.ru/spb/new
     * none → auth.wi-fi.ru/spb/gapi/auth/start
     * auth.wi-fi.ru/auth/init → auth.wi-fi.ru/spb/gapi/auth/init
     * auth.wi-fi.ru/auth/check → auth.wi-fi.ru/spb/gapi/auth/check
     * auth.wi-fi.ru/identification → auth.wi-fi.ru/spb/identification (?)
     */
    private Boolean spb = false;

    /**
     * Moscow Trains branch (MCC, CPPK, MCD)
     *
     * auth.wi-fi.ru → none
     * auth.wi-fi.ru/auth → auth.wi-fi.ru/new
     * none → auth.wi-fi.ru/gapi/auth/start
     * auth.wi-fi.ru/auth/init → auth.wi-fi.ru/gapi/auth/init
     * auth.wi-fi.ru/auth/check → auth.wi-fi.ru/gapi/auth/check
     * auth.wi-fi.ru/identification → auth.wi-fi.ru/identification (?)
     */
    private Boolean mcc = false;

    /**
     * Moscow Metro branch
     * 
     * auth.wi-fi.ru → none
     * auth.wi-fi.ru/auth → auth.wi-fi.ru/metro
     * none → auth.wi-fi.ru/gapi/auth/start
     * auth.wi-fi.ru/auth/init → auth.wi-fi.ru/gapi/auth/init
     * auth.wi-fi.ru/auth/check → auth.wi-fi.ru/gapi/auth/check
     * auth.wi-fi.ru/identification → auth.wi-fi.ru/identification (?)
     */
    private Boolean mosmetro = false;

    public MosMetroV2(final Context context, final ParsedResponse res) {
        super(context);

        /**
         * Checking Internet connection
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
                    vars.put("branch", "spb");
                    spb = true;
                } else if (uri.getPath().startsWith("/new")) {
                    vars.put("branch", "mcc");
                    mcc = true;
                } else if (uri.getPath().startsWith("/metro")) {
                    vars.put("branch", "metro");
                    mosmetro = true;
                } else {
                    vars.put("branch", "default");
                }

                Logger.log(Logger.LEVEL.DEBUG, "Branch: " + vars.get("branch"));

                if (uri.getQueryParameter("segment") != null) {
                    vars.put("segment", uri.getQueryParameter("segment"));
                } else {
                    vars.put("segment", "metro");
                }

                Logger.log(Logger.LEVEL.DEBUG, "Segment: " + vars.get("segment"));

                if (uri.getQueryParameter("mac") != null) {
                    vars.put("mac", uri.getQueryParameter("mac"));
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
         * Getting redirect
         * ⇒ GET http://auth.wi-fi.ru/?segment=... < redirect, segment
         * ⇐ JavaScript Redirect: http://auth.wi-fi.ru/auth?segment=...
         */
        add(new NamedTask(context.getString(R.string.auth_redirect)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                try {
                    if (!Patterns.WEB_URL.matcher(redirect).matches()) {
                        throw new ParseException("Invalid URL: " + redirect, 0);
                    }

                    ParsedResponse response = client.get(redirect, null, pref_retry_count);
                    Logger.log(Logger.LEVEL.DEBUG, response.getPageContent().outerHtml());
                    return true;
                } catch (IOException | ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_redirect)
                    ));
                    return false;
                }
            }
        });

        /**
         * Async: https://auth.wi-fi.ru/auth
         *        https://auth.wi-fi.ru/metro
         *        https://auth.wi-fi.ru/new
         *        https://auth.wi-fi.ru/spb/new
         * - Detect if device is not registered in the network (302 redirect to /identification)
         * - Parse CSRF token (if present)
         */
        add(new InterceptorTask(this, "https?://auth\\.wi-fi\\.ru/(auth|metro|(spb/)?new)(\\?.*)?") {
            @Override
            public ParsedResponse request(Client client, Client.METHOD method, String url, Map<String, String> params) throws IOException {
                client.followRedirects(false);
                ParsedResponse response = client.get(url, null, pref_retry_count);
                client.followRedirects(true);
                return response;
            }

            @NonNull @Override
            public ParsedResponse response(Client client, String url, ParsedResponse response) throws IOException {
                try {
                    String redirect = response.get300Redirect();

                    if (redirect.contains("/identification")) { // not registered
                        Logger.log(context.getString(R.string.error,
                                context.getString(R.string.auth_error_not_registered)
                        ));

                        vars.put("result", RESULT.NOT_REGISTERED);
                        running.set(false);
                        return response;
                    }

                    // Follow 3xx redirect
                    response = client.get(redirect, null, pref_retry_count);
                } catch (ParseException ignored) {}

                try {
                    String csrf_token = response.parseMetaContent("csrf-token");
                    Logger.log(Logger.LEVEL.DEBUG, "CSRF token: " + csrf_token);
                    client.setHeader(Client.HEADER_CSRF, csrf_token);
                } catch (ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, "CSRF token not found");
                }

                return response;
            }
        });

        /**
         * Following JavaScript redirect to the auth page
         * redirect = "scheme://host"
         * ⇒ GET http://auth.wi-fi.ru/auth?segment= < redirect + "/auth?segment=" + segment
         * ⇐ Form: method="post" action="/auto_auth" (captcha)
         * ⇐ AJAX: http://auth.wi-fi.ru/auth/init?segment=... (no captcha)
         */
        add(new NamedTask(context.getString(R.string.auth_auth_page)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                String url = ParsedResponse.removePathFromUrl(redirect);

                if (!spb && !mcc && !mosmetro) {
                    url += "/auth?segment=" + vars.get("segment");
                } else {
                    if (spb) {
                        url += "/spb";
                    }

                    url += "/gapi/auth/start?segment=" + vars.get("segment");

                    if (vars.containsKey("mac")) {
                        url += "&clientMac=" + vars.get("mac");
                    }
                }

                String prefix = "0:" + random.string(8) + ":";
                client.setCookie("http://auth.wi-fi.ru", "_ym_uid", random.string("0123456789", 19))
                      .setCookie("http://auth.wi-fi.ru", "_mts", prefix + random.string(11) + "~" + random.string(20))
                      .setCookie("http://auth.wi-fi.ru", "_mtp", prefix + random.string(21) + "_" + random.string(10));

                try {
                    ParsedResponse response = client.get(url, null, pref_retry_count);
                    Logger.log(Logger.LEVEL.DEBUG, response.toString());
                    return true;
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_auth_page)
                    ));
                    return false;
                }
            }
        });

        /**
         * Setting auth token
         * ⇒ GET http://auth.wi-fi.ru/auth/set_token?token= < random.string(6)
         * ⇐ 200 OK
         */
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                if (spb || mcc || mosmetro) return true;

                String token = new Randomizer(context).string(6);
                Logger.log(Logger.LEVEL.DEBUG, "Trying to set auth token: " + token);

                String url = ParsedResponse.removePathFromUrl(redirect);
                url += "/auth/set_token?token=" + token;

                try {
                    ParsedResponse response = client.get(url, null);
                    Logger.log(Logger.LEVEL.DEBUG, response.getPageContent().outerHtml());
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                }
                return true;
            }
        });

        if (!settings.getBoolean("pref_delay_always", false))
        add(new NamedTask(context.getString(R.string.notification_progress_waiting)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                random.delay(running); return true;
            }
        });

        /**
         * Sending login form
         * ⇒ POST http://auth.wi-fi.ru/auth/init?... < redirect, segment, TODO: mode=?
         * ⇒ Cookie: afVideoPassed = 0
         * ⇒ Header: CSRF-Token = ...
         * ⇐ JSON
         */
        add(new NamedTask(context.getString(R.string.auth_auth_form)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                String url = ParsedResponse.removePathFromUrl(redirect);

                if (!spb && !mcc && !mosmetro) {
                    url += "/auth/init?mode=0&segment=" + vars.get("segment");
                } else {
                    if (spb) {
                        url += "/spb";
                    }

                    url += "/gapi/auth/init?mode=0&segment=" + vars.get("segment");

                    if (vars.containsKey("mac")) {
                        url += "&clientMac=" + vars.get("mac");
                    }
                }

                try {
                    ParsedResponse response = client.post(url, null, pref_retry_count);
                    Logger.log(Logger.LEVEL.DEBUG, response.getPageContent().outerHtml());
                } catch (ProtocolException ignored) { // Too many follow-up requests
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_server)
                    ));
                    return false;
                }
                return true;
            }
        });

        /**
         * Checking auth state
         * ⇒ GET http://auth.wi-fi.ru/auth/check?segment=... < redirect, segment
         * ⇐ JSON result == true
         */
        add(new NamedTask(context.getString(R.string.auth_checking_connection)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                String url = ParsedResponse.removePathFromUrl(redirect);

                if (!spb && !mcc && !mosmetro) {
                    url += "/auth/check?segment=" + vars.get("segment");
                } else {
                    if (spb) {
                        url += "/spb";
                    }

                    url += "/gapi/auth/check?segment=" + vars.get("segment");
                }

                try {
                    JSONObject response = client.get(url, null, pref_retry_count).json();

                    Logger.log(Logger.LEVEL.DEBUG, response.toJSONString());

                    if (!((Boolean) response.get("result"))) {
                        throw new ParseException("Unexpected answer: false", 0);
                    }
                } catch (IOException|org.json.simple.parser.ParseException|ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_server)
                    ));
                    return false;
                }

                return true;
            }
        });

        /**
         * Checking Internet connection
         */
        add(new Task() {
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
    public static boolean match(ParsedResponse response) {
        String redirect;

        try {
            redirect = response.parseAnyRedirect();
        } catch (ParseException ex) {
            return false;
        }

        return redirect.contains(".wi-fi.ru") && !redirect.contains("login.wi-fi.ru");
    }
}
