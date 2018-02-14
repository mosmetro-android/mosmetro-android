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

package pw.thedrhax.mosmetro.httpclient;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import pw.thedrhax.util.Listener;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Randomizer;
import pw.thedrhax.util.Util;

public abstract class Client {
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_REFERER = "Referer";
    public static final String HEADER_CSRF = "X-CSRF-Token";
    public static final String HEADER_LOCATION = "Location";
    public static final String HEADER_UPGRADE_INSECURE_REQUESTS = "Upgrade-Insecure-Requests";

    protected Map<String,String> headers;
    protected Context context;
    protected Randomizer random;
    protected SharedPreferences settings;
    protected boolean random_delays = false;
    protected ParsedResponse last_response = new ParsedResponse("", "", 200, null);

    protected Client(Context context) {
        this.context = context;
        this.headers = new HashMap<>();
        this.random = new Randomizer(context);
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    // Settings methods
    public abstract Client trustAllCerts();
    public abstract Client followRedirects(boolean follow);

    public Client configure() {
        setTimeout(Util.getIntPreference(context, "pref_timeout", 5) * 1000);

        setHeader(HEADER_USER_AGENT, random.cached_useragent());
        setHeader(HEADER_ACCEPT_LANGUAGE, "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");

        return this;
    }

    @NonNull
    protected static String acceptByExtension(String url) {
        if (url.contains(".css")) {
            return "text/css,*/*;q=0.1";
        } else if (url.contains(".js") || url.contains(".woff")) {
            return "*/*";
        } else if (url.contains(".jpg") || url.contains(".jpeg") || url.contains(".gif")) {
            return "image/webp,image/apng,image/*,*/*;q=0.8";
        } else if (url.contains(".mp4")) {
            return "";
        } else {
            return "text/html,application/xhtml+xml," +
                   "application/xml;q=0.9,image/webp," +
                   "image/apng,*/*;q=0.8";
        }
    }

    public Client setHeader (String name, String value) {
        headers.put(name, value); return this;
    }

    public String getHeader (String name) {
        return headers.containsKey(name) ? headers.get(name) : null;
    }

    public Client resetHeaders () {
        headers = new HashMap<>(); return this;
    }

    public Client setDelaysEnabled(boolean enabled) {
        this.random_delays = enabled; return this;
    }

    public abstract Client setCookie(String url, String name, String value);
    public abstract Map<String,String> getCookies(String url);

    public Client setCookie(String url, String cookie) {
        String[] name_value = cookie.split("=");
        return setCookie(url, name_value[0], name_value.length > 1 ? name_value[1] : "");
    }

    public abstract Client setTimeout(int ms);

    // IO methods
    public abstract ParsedResponse get(String link, Map<String,String> params) throws IOException;
    public abstract ParsedResponse post(String link, Map<String,String> params) throws IOException;
    public abstract InputStream getInputStream(String link) throws IOException;

    private ParsedResponse saveResponse(ParsedResponse response) {
        this.last_response = response;

        setHeader(Client.HEADER_REFERER, last_response.getURL());

        if (settings.getBoolean("pref_load_resources", true)) {
            last_response.loadResources(Client.this);
        }

        return response;
    }

    @NonNull
    public ParsedResponse response() {
        return last_response;
    }

    // Retry methods
    public ParsedResponse get(final String link, final Map<String,String> params,
                      int retries) throws IOException {
        return new RetryOnException<ParsedResponse>() {
            @Override
            public ParsedResponse body() throws IOException {
                if (random_delays) {
                    random.delay(running);
                }
                return saveResponse(get(link, params));
            }
        }.run(retries);
    }
    public ParsedResponse post(final String link, final Map<String,String> params,
                       int retries) throws IOException {
        return new RetryOnException<ParsedResponse>() {
            @Override
            public ParsedResponse body() throws IOException {
                if (random_delays) {
                    random.delay(running);
                }
                return saveResponse(post(link, params));
            }
        }.run(retries);
    }
    public InputStream getInputStream(final String link, int retries) throws IOException {
        return new RetryOnException<InputStream>() {
            @Override
            public InputStream body() throws IOException {
                return getInputStream(link);
            }
        }.run(retries);
    }

    // Cancel current request
    public abstract void stop();

    // Convert methods
    protected static String requestToString (Map<String,String> params) {
        StringBuilder params_string = new StringBuilder();

        if (params != null)
            for (Map.Entry<String,String> entry : params.entrySet())
                params_string
                        .append(params_string.length() == 0 ? "?" : "&")
                        .append(entry.getKey())
                        .append("=")
                        .append(entry.getValue());

        return params_string.toString();
    }

    protected final Listener<Boolean> running = new Listener<Boolean>(true) {
        @Override
        public void onChange(Boolean new_value) {
            if (!new_value) {
                stop();
            }
        }
    };

    public Client setRunningListener(Listener<Boolean> master) {
        running.subscribe(master); return this;
    }

    private abstract class RetryOnException<T> {
        T run(int retries) throws IOException {
            IOException last_ex = null;
            for (int i = 0; i < retries; i++) {
                try {
                    return body();
                } catch (IOException ex) {
                    last_ex = ex;
                    if (running.get()) {
                        Logger.log(Logger.LEVEL.DEBUG, ex.toString());
                        Logger.log(Client.this,
                                "Retrying request (try " + (i+1) + " out of " + retries + ")"
                        );
                        SystemClock.sleep(1000);
                    } else {
                        Logger.log(Client.this, "Giving up (interrupted)");
                        break;
                    }
                }
            }
            if (last_ex != null) {
                throw last_ex;
            } else {
                throw new IOException("Unknown exception (retries=" + retries + ")");
            }
        }
        public abstract T body() throws IOException;
    }
}
