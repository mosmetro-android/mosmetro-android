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
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLException;

import pw.thedrhax.mosmetro.authenticator.InterceptorTask;
import pw.thedrhax.util.Listener;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Randomizer;
import pw.thedrhax.util.Util;

public abstract class Client {
    public enum METHOD { GET, POST }

    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";
    public static final String HEADER_ACAO = "Access-Control-Allow-Origin";
    public static final String HEADER_ACAC = "Access-Control-Allow-Credentials";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_REFERER = "Referer";
    public static final String HEADER_CSRF = "X-CSRF-Token";
    public static final String HEADER_LOCATION = "Location";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_UPGRADE_INSECURE_REQUESTS = "Upgrade-Insecure-Requests";

    public final List<InterceptorTask> interceptors = new LinkedList<>();

    private boolean intercepting = false;
    protected Map<String,String> headers;
    protected Context context;
    protected Randomizer random;
    protected SharedPreferences settings;
    protected boolean random_delays = false;

    protected Client(Context context) {
        this.context = context;
        this.headers = new HashMap<>();
        this.random = new Randomizer(context);
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    // Settings methods
    public abstract Client trustAllCerts();
    public abstract Client followRedirects(boolean follow);
    public abstract Client customDnsEnabled(boolean enabled);

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

    public Client setCookies(String url, Map<String,String> cookies) {
        for (String name : cookies.keySet()) {
            setCookie(url, name, cookies.get(name));
        }
        return this;
    }

    public abstract Client setTimeout(int ms);

    // IO methods
    protected abstract HttpResponse request(HttpRequest request) throws IOException;

    protected HttpResponse requestWithRetries(HttpRequest request) throws IOException {
        return new RetryOnException<HttpResponse>() {
            @Override
            public HttpResponse body() throws IOException {
                if (random_delays) {
                    if (!random.delay(running)) {
                        throw new InterruptedIOException();
                    }
                }
                return request(request);
            }
        }.run(request.getTries());
    }

    private HttpResponse interceptedRequest(HttpRequest request) throws IOException {
        HttpResponse response = null;

        try {
            if (!intercepting) {
                intercepting = true;

                for (InterceptorTask i : interceptors) {
                    if (i.match(request.getUrl())) {
                        response = i.request(this, request);

                        if (response != null) {
                            Logger.log(this, "Request intercepted by " + i.toString());
                            break;
                        }
                    }
                }
            }

            if (response == null) {
                response = requestWithRetries(request);
            }

            for (InterceptorTask i : interceptors) {
                if (i.match(request.getUrl())) {
                    response = i.response(this, request, response);
                    Logger.log(this, "Response intercepted by " + i.toString());
                }
            }
        } finally {
            intercepting = false;
        }

        if (response == null) {
            return new HttpResponse(request, "");
        }

        String type = response.getResponseHeader(HEADER_CONTENT_TYPE.toLowerCase());

        if (type != null && type.startsWith("text/html")) {
            if (!response.getUrl().isEmpty()) {
                setHeader(Client.HEADER_REFERER, response.getUrl());
            }
        }

        return response;
    }

    public HttpRequest get(String link) {
        return new HttpRequest(this, METHOD.GET, link);
    }

    public HttpRequest get(String link, Map<String,String> form) {
        return new HttpRequest(this, METHOD.GET, link + '?' + requestToString(form));
    }

    public HttpRequest post(String link, String body, String type) {
        return new HttpRequest(this, METHOD.POST, link).setBody(body, type);
    }

    public HttpRequest post(String link, Map<String,String> form) {
        return new HttpRequest(this, METHOD.POST, link)
                .setBody(requestToString(form), "application/x-www-form-urlencoded");
    }

    public HttpResponse execute(HttpRequest request) throws IOException {
        return interceptedRequest(request);
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
                        .append(URLEncoder.encode(entry.getValue()));

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
        T run(int tries) throws IOException {
            IOException last_ex;

            try {
                return body();
            } catch (IOException ex) {
                last_ex = ex;

                for (int i = 2; i <= tries; i++) {
                    Logger.log(Logger.LEVEL.DEBUG, ex.toString());

                    if (last_ex instanceof SSLException) {
                        throw last_ex;
                    }

                    if (!running.sleep(1000)) {
                        throw new InterruptedIOException();
                    }

                    Logger.log(Client.this,
                            "Retrying request (try " + i + " out of " + tries + ")"
                    );

                    try {
                        return body();
                    } catch (IOException ex1) {
                        last_ex = ex1;
                    }
                }
            }

            throw last_ex;
        }

        public abstract T body() throws IOException;
    }
}
