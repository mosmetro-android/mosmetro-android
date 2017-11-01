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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Patterns;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.ProtocolException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.authenticator.Task;
import pw.thedrhax.mosmetro.authenticator.captcha.CaptchaRecognitionProxy;
import pw.thedrhax.mosmetro.authenticator.captcha.CaptchaRequest;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Logger;

/**
 * The MosMetroV2 class supports the actual version of the MosMetro algorithm.
 *
 * Detection: Meta-redirect contains ".wi-fi.ru" with any 3rd level domain (except "login").
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see Provider
 */

public class MosMetroV2 extends Provider {
    private String redirect = "http://auth.wi-fi.ru/";

    public MosMetroV2(final Context context) {
        super(context);

        /**
         * Checking Internet connection for a first time
         * ⇒ GET http://wi-fi.ru
         * ⇐ Meta-redirect: http://auth.wi-fi.ru/?segment=... > redirect, segment
         */
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                Logger.log(context.getString(R.string.auth_checking_connection));

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
         * Checking if device is not registered
         * redirect ~= auth.wi-fi.ru/identification?segment=...
         */
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                if (redirect.contains(".wi-fi.ru/identification")) {
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_not_registered)
                    ));
                    vars.put("result", RESULT.NOT_REGISTERED);
                    return false;
                }
                return true;
            }
        });

        /**
         * Checking for bad redirect
         * redirect ~= welcome.wi-fi.ru
         */
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                if (redirect.contains("welcome.wi-fi.ru")) {
                    Logger.log(Logger.LEVEL.DEBUG, "Found redirect to welcome.wi-fi.ru!");
                    try {
                        client.get(redirect, null, pref_retry_count);
                        Logger.log(Logger.LEVEL.DEBUG, client.getPage());
                    } catch (IOException ex) {
                        Logger.log(Logger.LEVEL.DEBUG, ex);
                    }

                    redirect = Uri.parse(redirect).buildUpon()
                            .authority("auth.wi-fi.ru")
                            .build().toString();
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
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                Logger.log(context.getString(R.string.auth_redirect));

                try {
                    if (!Patterns.WEB_URL.matcher(redirect).matches()) {
                        throw new ParseException("Invalid URL: " + redirect, 0);
                    }

                    client.get(redirect, null, pref_retry_count);
                    Logger.log(Logger.LEVEL.DEBUG, client.getPageContent().outerHtml());
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
         * Following JavaScript redirect to the auth page
         * redirect = "scheme://host"
         * ⇒ GET http://auth.wi-fi.ru/auth?segment= < redirect + "/auth?segment=" + segment
         * ⇐ Form: method="post" action="/auto_auth" (captcha)
         * ⇐ AJAX: http://auth.wi-fi.ru/auth/init?segment=... (no captcha)
         */
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                Logger.log(context.getString(R.string.auth_auth_page));

                Uri redirect_uri = Uri.parse(redirect);
                redirect = redirect_uri.getScheme() + "://" + redirect_uri.getHost();

                try {
                    client.get(redirect + "/auth?segment=" + vars.get("segment"), null, pref_retry_count);
                    Logger.log(Logger.LEVEL.DEBUG, client.getPageContent().outerHtml());
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
         * Asking user to solve the CAPTCHA and send the form
         */
        Task captcha_task = new Task() {
            private Element form = null;

            private boolean isCaptchaRequested() {
                return client.getPageContent().location().contains("auto_auth");
            }

            private boolean submit(String code) {
                Logger.log(context.getString(R.string.auth_request));

                Map<String,String> fields = Client.parseForm(form);
                fields.put("_rucaptcha", code);

                try {
                    client.post(redirect + form.attr("action"), fields, pref_retry_count);
                    Logger.log(Logger.LEVEL.DEBUG, client.getPageContent().toString());
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_server)
                    ));
                }

                return !isCaptchaRequested();
            }

            private boolean bypass_backdoor() throws IOException {
                Logger.log(context.getString(R.string.auth_ban_bypass_backdoor));

                Client tmp_client = new OkHttp(context)
                        .setTimeout(1000)
                        .resetHeaders()
                        .setHeader(Client.HEADER_USER_AGENT,
                                new String(Base64.decode(
                                        "QXV0b01vc01ldHJvV2lmaS8xLjUuMyAoTGlud" +
                                        "Xg7IEFuZHJvaWQgNC40LjQ7IEEwMTIzIEJ1aW" +
                                        "xkL0tUVTg0UCk=",
                                        Base64.DEFAULT
                                ))
                        )
                        .setHeader(Client.HEADER_REFERER,
                                new String(Base64.decode(
                                        "aHR0cDovL25ldGNoZWNrLndpLWZpLnJ1Lw==",
                                        Base64.DEFAULT
                                ))
                        );

                try {
                    tmp_client.get(
                            new String(Base64.decode(
                                    "aHR0cHM6Ly9hbW13LndpLWZpLnJ1L25ldGluZm8vYXV0aA==",
                                    Base64.DEFAULT
                            )), null
                    );
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                }

                if (tmp_client.getResponseCode() == 401) {
                    tmp_client.get(
                            new String(Base64.decode(
                                    "aHR0cHM6Ly9hbW13LndpLWZpLnJ1L25ldGluZm8vcmVnCg==",
                                    Base64.DEFAULT
                            )), null
                    );

                    tmp_client.get(
                            new String(Base64.decode(
                                    "aHR0cHM6Ly9hbW13LndpLWZpLnJ1L25ldGluZm8vYXV0aA==",
                                    Base64.DEFAULT
                            )), null
                    );
                }

                return tmp_client.getResponseCode() == 200 && isConnected();
            }

            private boolean bypass_mcc(HashMap<String, Object> vars) throws IOException {
                Logger.log(context.getString(R.string.auth_ban_bypass_mcc));
                vars.put("segment", "mcc");
                client.get(redirect + "/auth?segment=" + vars.get("segment"), null, pref_retry_count);
                Logger.log(Logger.LEVEL.DEBUG, client.getPageContent().outerHtml());
                return !isCaptchaRequested();
            }

            @Override
            public boolean run(HashMap<String, Object> vars) {
                if (!isCaptchaRequested()) return true;

                // Parsing CAPTCHA form
                form = client.getPageContent().getElementsByTag("form").first();

                if (form == null) { // No CAPTCHA form found => level 2 block
                    Logger.log(context.getString(R.string.auth_ban_message));

                    if (!settings.getBoolean("pref_captcha_backdoor", true)) {
                        return false;
                    }

                    try {
                        if (bypass_mcc(vars)) {
                            Logger.log(context.getString(R.string.auth_ban_bypass_success));
                            vars.put("captcha", "mcc");
                            return true;
                        } else {
                            throw new Exception("Doesn't work");
                        }
                    } catch (Exception ex) { // Exception type doesn't matter here
                        Logger.log(Logger.LEVEL.DEBUG, ex);
                        Logger.log(context.getString(R.string.auth_ban_bypass_fail));
                    }

                    try {
                        if (bypass_backdoor()) {
                            Logger.log(context.getString(R.string.auth_ban_bypass_success));
                            vars.put("result", RESULT.CONNECTED);
                            vars.put("captcha", "backdoor");
                            return false;
                        } else {
                            throw new Exception("Server didn't accept the backdoor");
                        }
                    } catch (Exception ex) { // Exception type doesn't matter here
                        Logger.log(Logger.LEVEL.DEBUG, ex);
                        Logger.log(context.getString(R.string.auth_ban_bypass_fail));
                        vars.put("result", RESULT.ERROR);
                        return true;
                    }
                }

                Logger.log(context.getString(R.string.auth_captcha_requested));

                // Parsing captcha URL
                Element captcha_img = form.getElementsByTag("img").first();
                String captcha_url = redirect + captcha_img.attr("src");

                // Try to recognize CAPTCHA within pref_retry_count attempts
                Bitmap captcha = null;
                String code = null;
                CaptchaRecognitionProxy cr = new CaptchaRecognitionProxy(context)
                        .setRunningListener(running);
                for (int i = 0; i < pref_retry_count + 1 && running.get(); i++) {
                    // Download CAPTCHA
                    try {
                        captcha = BitmapFactory.decodeStream(
                                client.getInputStream(captcha_url, pref_retry_count)
                        );

                        if (captcha == null)
                            throw new Exception("CAPTCHA is null!");
                    } catch (Exception ex) { // Exception type doesn't matter here
                        continue;
                    }

                    if (!cr.isModuleAvailable()) {
                        Logger.log(Logger.LEVEL.DEBUG, "CAPTCHA recognition module is not installed");
                        break;
                    }

                    code = cr.recognize(captcha); // neural magic

                    if (code == null) {
                        Logger.log(context.getString(R.string.auth_captcha_recognition_failed));
                        continue;
                    }

                    if (i == pref_retry_count) break; // Leave the last unsolved CAPTCHA to user

                    if (submit(code)) {
                        Logger.log(this, String.format(Locale.ENGLISH,
                                "CAPTCHA solved: tries=%d, code=%s", i+1, code
                        ));
                        Logger.log(Logger.LEVEL.INFO,
                                context.getString(R.string.auth_captcha_recognized)
                        );
                        vars.put("captcha", "recognized");
                        return true;
                    }
                }

                if (captcha == null) {
                    Logger.log(context.getString(
                            R.string.error, context.getString(R.string.error_image)
                    ));
                    vars.put("result", RESULT.ERROR);
                    return false;
                }

                if (isStopped()) return false;

                // Asking user to enter the CAPTCHA
                vars.putAll(
                        new CaptchaRequest(context)
                                .setRunningListener(running)
                                .getResult(captcha, code)
                );

                // Check the answer
                code = (String) vars.get("captcha_code");
                if (code == null || code.isEmpty()) {
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_captcha)
                    ));
                    vars.put("result", RESULT.ERROR);
                    return false;
                } else {
                    Logger.log(Logger.LEVEL.DEBUG, String.format(
                            context.getString(R.string.auth_captcha_result), code
                    ));
                    vars.put("captcha", "entered");
                }

                // Send code to server and proceed
                return submit(code);
            }
        };
        add(captcha_task);

        /**
         * Sending login form
         * ⇒ POST http://auth.wi-fi.ru/auth/init?... < redirect, segment, TODO: mode=?
         * ⇒ Cookie: afVideoPassed = 0
         * ⇒ Header: CSRF-Token = ...
         * ⇐ JSON
         */
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                Logger.log(context.getString(R.string.auth_auth_form));

                try {
                    String csrf_token = client.parseMetaContent("csrf-token");
                    client.setHeader(Client.HEADER_CSRF, csrf_token);
                    client.setCookie(redirect, "afVideoPassed", "0");

                    client.post(
                            redirect + "/auth/init?mode=0&segment=" + vars.get("segment"),
                            null, pref_retry_count
                    );
                    Logger.log(Logger.LEVEL.DEBUG, client.getPageContent().outerHtml());
                } catch (ProtocolException ignored) { // Too many follow-up requests
                } catch (IOException | ParseException ex) {
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
         * Expect delayed CAPTCHA request on Moscow Central Circle
         */
        add(captcha_task);

        /**
         * Checking Internet connection
         * JSON result > status
         */
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                Logger.log(context.getString(R.string.auth_checking_connection));

                try {
                    JSONObject response = (JSONObject)new JSONParser().parse(client.getPage());

                    if ((Boolean)response.get("result") && isConnected()) {
                        Logger.log(context.getString(R.string.auth_connected));
                        vars.put("result", RESULT.CONNECTED);
                        return true;
                    } else {
                        Logger.log(context.getString(R.string.error,
                                context.getString(R.string.auth_error_connection)
                        ));
                        return false;
                    }
                } catch (org.json.simple.parser.ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    return false;
                }
            }
        });
    }

    @Override
    public boolean isConnected() {
        Client client = new OkHttp(context).followRedirects(false);
        try {
            client.get("http://mosgortrans.ru", null, pref_retry_count);
        } catch (IOException ex) {
            Logger.log(Logger.LEVEL.DEBUG, ex);
            return false;
        }

        // Detect midsession
        try {
            String location = client.get300Redirect();
            if (location.contains("midsession")) {
                Logger.log(Logger.LEVEL.DEBUG, "Detected midsession: " + location);
                return true;
            }
        } catch (ParseException ignored) {}

        try {
            redirect = client.parseMetaRedirect();
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
     * @param client    Client instance to get the information from. Provider.find()
     *                  will execute one request to be analyzed by this method.
     * @return          True if response matches this Provider implementation.
     */
    public static boolean match(Client client) {
        try {
            String redirect = client.parseMetaRedirect();
            return redirect.contains(".wi-fi.ru") && !redirect.contains("login.wi-fi.ru");
        } catch (ParseException ex) {
            return false;
        }
    }
}
