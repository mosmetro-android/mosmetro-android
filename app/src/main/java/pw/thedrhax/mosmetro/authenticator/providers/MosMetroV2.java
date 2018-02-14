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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Patterns;

import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.ProtocolException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.NamedTask;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.authenticator.ScriptedWebViewTask;
import pw.thedrhax.mosmetro.authenticator.Task;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.ParsedResponse;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Randomizer;

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

        final Calendar cal = Calendar.getInstance();
        Task auth_webview = new ScriptedWebViewTask(this,
                context.getString(R.string.auth_webview_message),
                "https://auth.wi-fi.ru/",
                "if (document.URL == 'https://auth.wi-fi.ru/') { 'redirect'; } " +
                       "else if (document.URL == 'https://auth.wi-fi.ru/auth') { 'SUCCESS'; } " +
                       "else { document.URL; 'ERROR'; }") {

            @Override
            public boolean onResult(@Nullable Intent intent) {
                if (intent == null) {
                    return false;
                }

                boolean success = false;
                if (intent.hasExtra("result")) {
                    success = "SUCCESS".equals(intent.getStringExtra("result"));
                }

                if (success) {
                    settings.edit()
                            .putInt("webview_last_day", cal.get(Calendar.DAY_OF_WEEK))
                            .apply();
                }

                return success;
            }
        };

        /**
         * Temporary workaround to avoid provider block
         */
        if (Build.VERSION.SDK_INT >= 19)
            if (settings.getInt("webview_last_day", 50) != cal.get(Calendar.DAY_OF_WEEK)
                    || settings.getBoolean("pref_webview_always", false))
                if (settings.getBoolean("pref_webview_enabled", true))
                    add(auth_webview);

        /**
         * Checking Internet connection for a first time
         * ⇒ GET http://wi-fi.ru
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
                        Logger.log(Logger.LEVEL.DEBUG, client.response().getPage());
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
        add(new NamedTask(context.getString(R.string.auth_redirect)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                try {
                    if (!Patterns.WEB_URL.matcher(redirect).matches()) {
                        throw new ParseException("Invalid URL: " + redirect, 0);
                    }

                    client.get(redirect, null, pref_retry_count);
                    Logger.log(Logger.LEVEL.DEBUG, client.response().getPageContent().outerHtml());
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
        add(new NamedTask(context.getString(R.string.auth_auth_page)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                redirect = ParsedResponse.removePathFromUrl(redirect);

                String prefix = "0:" + random.string(8) + ":";
                client.setCookie("http://auth.wi-fi.ru", "_ym_uid", random.string("0123456789", 19))
                      .setCookie("http://auth.wi-fi.ru", "_mts", prefix + random.string(11) + "~" + random.string(20))
                      .setCookie("http://auth.wi-fi.ru", "_mtp", prefix + random.string(21) + "_" + random.string(10));
                try {
                    client.get(
                            redirect + "/auth?segment=" + vars.get("segment"),
                            null, pref_retry_count
                    );
                    Logger.log(Logger.LEVEL.DEBUG, client.response().getPageContent().outerHtml());
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
         * Detect ban (redirect to /auto_auth)
         */
        Task captcha_task = new Task() {
            private boolean isCaptchaRequested() {
                return client.response().getPageContent().location().contains("auto_auth");
            }

            @Override
            public boolean run(HashMap<String, Object> vars) {
                if (!isCaptchaRequested()) return true;

                Logger.log(context.getString(R.string.auth_ban_message));

                // Increase ban counter
                settings.edit()
                        .putInt("metric_ban_count", settings.getInt("metric_ban_count", 0) + 1)
                        .apply();

                context.sendBroadcast(new Intent("pw.thedrhax.mosmetro.event.MosMetroV2.BANNED"));
                vars.put("result", RESULT.ERROR);
                return false;
            }
        };
        add(captcha_task);

        /**
         * Setting auth token
         * ⇒ GET http://auth.wi-fi.ru/auth/set_token?token= < random.string(6)
         * ⇐ 200 OK
         */
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                String token = new Randomizer(context).string(6);
                Logger.log(Logger.LEVEL.DEBUG, "Trying to set auth token: " + token);

                try {
                    ParsedResponse response = client.get(
                            redirect + "/auth/set_token?token=" + token, null
                    );
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
                try {
                    String csrf_token = client.response().parseMetaContent("csrf-token");
                    client.setHeader(Client.HEADER_CSRF, csrf_token);

                    client.post(
                            redirect + "/auth/init?mode=0&segment=" + vars.get("segment"),
                            null, pref_retry_count
                    );
                    Logger.log(Logger.LEVEL.DEBUG, client.response().getPageContent().outerHtml());
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
        add(new NamedTask(context.getString(R.string.auth_checking_connection)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                try {
                    JSONObject response = client.get(
                            redirect + "/auth/check?segment=" + vars.get("segment"),
                            null, pref_retry_count
                    ).json();

                    if ((Boolean) response.get("result")) {
                        Logger.log(context.getString(R.string.auth_connected));
                        vars.put("result", RESULT.CONNECTED);
                        return true;
                    }
                } catch (IOException|org.json.simple.parser.ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                }

                Logger.log(context.getString(R.string.error,
                        context.getString(R.string.auth_error_connection)
                ));
                return false;
            }
        });

        /**
         * Try to disable midsession with non-blocking request
         */
        if (settings.getBoolean("pref_mosmetro_midsession", true))
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                new AsyncTask<Void,Void,Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        Client tmp_client = new OkHttp(context)
                                .setRunningListener(running)
                                .followRedirects(false);

                        random.delay(running);

                        try {
                            String location = tmp_client
                                    .get("http://ya.ru", null, pref_retry_count)
                                    .get300Redirect();

                            if (!location.contains("midsession")) return null;

                            Logger.log(Logger.LEVEL.DEBUG, "Detected midsession: " + location);

                            tmp_client.get(location, null, pref_retry_count);
                            Logger.log(Logger.LEVEL.DEBUG, tmp_client.response().toString());
                        } catch (IOException|ParseException ex) {
                            Logger.log(Logger.LEVEL.DEBUG, ex);
                        }

                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                return true;
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
