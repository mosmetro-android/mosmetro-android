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
import androidx.annotation.Nullable;

import android.util.Patterns;

import com.jayway.jsonpath.DocumentContext;

import java.io.IOException;
import java.net.ProtocolException;
import java.text.ParseException;
import java.util.HashMap;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.FinalConnectionCheckTask;
import pw.thedrhax.mosmetro.authenticator.FollowRedirectsTask;
import pw.thedrhax.mosmetro.authenticator.InitialConnectionCheckTask;
import pw.thedrhax.mosmetro.authenticator.InterceptorTask;
import pw.thedrhax.mosmetro.authenticator.NamedTask;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.authenticator.Task;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.Headers;
import pw.thedrhax.mosmetro.httpclient.HttpRequest;
import pw.thedrhax.mosmetro.httpclient.HttpResponse;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Randomizer;

/**
 * The MosMetroV2 class implements support for auth.wi-fi.ru algorithm.
 *
 * Detection: Meta-redirect contains:
 *   - host: auth.wi-fi.ru
 *   - path:
 *     - /auth              (default)
 *     - /                  (metro)
 *     - /new               (metro)
 *     - /spb               (spb)
 *   - search:
 *     - segment
 *     - mac / client_mac   (optional)
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see Provider
 */

public class MosMetroV2 extends Provider {
    private String redirect = "https://auth.wi-fi.ru/?segment=metro";

    // TODO: Split branches into sub-providers

    /**
     * Saint-Petersburg branch
     *
     * auth.wi-fi.ru → none
     * auth.wi-fi.ru/auth → auth.wi-fi.ru/spb
     * none → auth.wi-fi.ru/spb/gapi/auth/start
     * auth.wi-fi.ru/auth/init → auth.wi-fi.ru/spb/gapi/auth/init
     * auth.wi-fi.ru/auth/check → auth.wi-fi.ru/spb/gapi/auth/check
     * auth.wi-fi.ru/identification → auth.wi-fi.ru/spb/identification
     */
    private Boolean spb = false;

    /**
     * Moscow Metro branch
     * 
     * auth.wi-fi.ru → none
     * auth.wi-fi.ru/auth → auth.wi-fi.ru(/|/new)?
     * none → auth.wi-fi.ru/gapi/auth/start
     * auth.wi-fi.ru/auth/init → auth.wi-fi.ru/gapi/auth/init
     * auth.wi-fi.ru/auth/check → auth.wi-fi.ru/gapi/auth/check
     * auth.wi-fi.ru/identification → auth.wi-fi.ru/identification
     */
    private Boolean mosmetro = false;

    public MosMetroV2(final Context context, final HttpResponse res) {
        super(context);

        /**
         * Checking Internet connection
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

                Uri uri = Uri.parse(redirect);
                String path = uri.getPath();

                if (path.startsWith("/auth")) {
                    vars.put("branch", "default");
                } else if (path.startsWith("/spb")) {
                    vars.put("branch", "spb");
                    spb = true;
                } else if (path.isEmpty() || path.equals("/") || path.startsWith("/new")) {
                    String dn = uri.getQueryParameter("dn");
                    boolean ruckus = dn != null && dn.contains("ruckus");

                    vars.put("branch", ruckus ? "metro-ruckus" : "metro");
                    mosmetro = true;
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

                if (uri.getQueryParameter("mac") != null) { // from cppk
                    vars.put("mac", uri.getQueryParameter("mac"));
                }

                if (uri.getQueryParameter("client_mac") != null) { // from metro
                    vars.put("mac", uri.getQueryParameter("client_mac"));
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

                    HttpResponse response = client.get(redirect).retry().execute();
                    Logger.log(Logger.LEVEL.DEBUG, response.toString());

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
         * - Parse CSRF token (if present)
         */
        add(new InterceptorTask("https?://auth\\.wi-fi\\.ru/(auth|metro|(spb/)?new)(\\?.*)?") {

            @NonNull @Override
            public HttpResponse response(Client client, HttpRequest request, HttpResponse response) throws IOException {
                try {
                    String csrf_token = response.parseMetaContent("csrf-token");
                    Logger.log(Logger.LEVEL.DEBUG, "CSRF token: " + csrf_token);
                    client.headers.setHeader(Headers.CSRF, csrf_token);
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
                String url = HttpResponse.removePathFromUrl(redirect);

                if (!spb && !mosmetro) {
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
                    HttpResponse response = client.get(url).retry().execute();
                    
                    if (mosmetro || spb) { // expecting JSON
                        Logger.log(Logger.LEVEL.DEBUG, response.toHeaderString());

                        DocumentContext json = response.jsonpath();
                        json.delete("$.data.segmentParams.auth");
                        json.delete("$.data.userParams");
                        Logger.log(Logger.LEVEL.DEBUG, json.jsonString());

                        String afterAuth = json.read("$.data.segmentParams.common.redirectUrl.afterAuth");
                        try {
                            if (afterAuth == null) throw new ParseException("URL is null", 0);
                            afterAuth = HttpResponse.absolutePathToUrl(url, afterAuth); // throws ParseException
                            vars.put("post_auth_redirect", afterAuth);
                            Logger.log(Logger.LEVEL.DEBUG, "Post-auth redirect: " + afterAuth);
                        } catch (ParseException ex) {
                            Logger.log(Logger.LEVEL.DEBUG, ex);
                            Logger.log(Logger.LEVEL.DEBUG, "Warning: Unable to parse post-auth redirect");
                        }
                    } else {
                        Logger.log(Logger.LEVEL.DEBUG, response.toString());
                    }

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
                if (spb || mosmetro) return true;

                String token = new Randomizer(context).string(6);
                Logger.log(Logger.LEVEL.DEBUG, "Trying to set auth token: " + token);

                String url = HttpResponse.removePathFromUrl(redirect);
                url += "/auth/set_token?token=" + token;

                try {
                    HttpResponse response = client.get(url).execute();
                    Logger.log(Logger.LEVEL.DEBUG, response.getPageContent().outerHtml());
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                }
                return true;
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
                String url = HttpResponse.removePathFromUrl(redirect);

                HashMap<String,String> params = new HashMap<>();
                params.put("mode", "0");
                params.put("segment", (String) vars.get("segment"));

                if (!spb && !mosmetro) {
                    url += "/auth/init";
                } else {
                    if (spb) {
                        url += "/spb";
                    }

                    url += "/gapi/auth/init";
                }

                try {
                    HttpResponse res = client.post(url, params).retry().execute();
                    Logger.log(Logger.LEVEL.DEBUG, res.toString());

                    DocumentContext data = res.jsonpath();

                    String error_code = data.read("$.auth_error_code");
                    if (error_code != null && error_code.startsWith("err_device_not_identified")) {
                        Logger.log(context.getString(R.string.error,
                                context.getString(R.string.auth_error_not_registered)
                        ));

                        vars.put("result", RESULT.NOT_REGISTERED);
                        return false;
                    }
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
                String url = HttpResponse.removePathFromUrl(redirect);

                if (!spb && !mosmetro) {
                    url += "/auth/check?segment=" + vars.get("segment");
                } else {
                    if (spb) {
                        url += "/spb";
                    }

                    url += "/gapi/auth/check?segment=" + vars.get("segment");
                }

                try {
                    HttpResponse res = client.get(url).retry().execute();
                    Logger.log(Logger.LEVEL.DEBUG, res.toString());
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

        add(new FollowRedirectsTask(this) {
            @Nullable @Override
            public String getInitialRedirect(HashMap<String, Object> vars) {
                return (String) vars.get("post_auth_redirect");
            }

            @Override
            public void getLastRedirect(String url) {

            }
        }.setIgnoreErrors(true).setSwitchProviders(true));

        add(new FinalConnectionCheckTask(this));
    }

    /**
     * Checks if current network is supported by this Provider implementation.
     * @param response  Instance of ParsedResponse.
     * @return          True if response matches this Provider implementation.
     */
    public static boolean match(HttpResponse response) {
        String redirect;

        try {
            redirect = response.parseAnyRedirect();
        } catch (ParseException ex) {
            return false;
        }

        return redirect.matches("^https?://auth\\.wi-fi\\.ru/(auth|new|spb/)?(\\?.*)?$");
    }
}
