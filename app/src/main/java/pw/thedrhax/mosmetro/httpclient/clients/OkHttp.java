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

package pw.thedrhax.mosmetro.httpclient.clients;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.DnsClient;
import pw.thedrhax.mosmetro.httpclient.Headers;
import pw.thedrhax.mosmetro.httpclient.HttpRequest;
import pw.thedrhax.mosmetro.httpclient.HttpResponse;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.WifiUtils;

public class OkHttp extends Client {
    private OkHttpClient client;
    private WifiUtils wifi;
    private Call last_call = null;

    public OkHttp(Context context) {
        super(context);
        wifi = new WifiUtils(context);

        client = new OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .connectionPool(new ConnectionPool(0, 1, TimeUnit.SECONDS))
                .cookieJar(new InterceptedCookieJar())
                .build();

        configure();
    }

    @Override
    public Client trustAllCerts() {
        X509TrustManager tm = new X509TrustManager() {
            @SuppressLint("TrustAllX509TrustManager")
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {}

            @SuppressLint("TrustAllX509TrustManager")
            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {}

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }
        };

        SSLSocketFactory socketFactory;
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{tm}, new java.security.SecureRandom());
            socketFactory = sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException ex) {
            return this;
        }

        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            @SuppressLint("BadHostnameVerifier")
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        client = client.newBuilder()
                .hostnameVerifier(hostnameVerifier)
                .sslSocketFactory(socketFactory, tm)
                .build();

        return this;
    }

    @Override
    public Client setCookie(String url, String name, String value) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        List<Cookie> url_cookies = new ArrayList<>();
        url_cookies.add(Cookie.parse(httpUrl, name + "=" + value));
        client.cookieJar().saveFromResponse(httpUrl, url_cookies);
        return this;
    }

    public Map<String, String> getCookies(String url) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        Map<String,String> result = new HashMap<>();
        List<Cookie> url_cookies = client.cookieJar().loadForRequest(httpUrl);
        if (url_cookies != null)
            for (Cookie cookie : url_cookies) {
                result.put(cookie.name(), cookie.value());
            }
        return result;
    }

    @Override
    public Client setTimeout(int ms) {
        if (ms == 0) return this;

        client = client.newBuilder()
                .callTimeout(ms, TimeUnit.MILLISECONDS)
                .build();

        return this;
    }

    @Override
    public Client customDnsEnabled(boolean enabled) {
        Dns dns;

        if (enabled && wifi.isPrivateDnsActive()) {
            dns = new DnsClient(context);
        } else {
            dns = Dns.SYSTEM;
        }

        client = client.newBuilder()
                 .dns(dns)
                 .build();

        return this;
    }

    protected HttpResponse request(HttpRequest request) throws IOException {
        if (!running.get()) throw new InterruptedIOException();

        Request.Builder builder = new Request.Builder().url(request.getUrl());

        // Choose appropriate request method
        switch (request.getMethod()) {
            case GET:
                builder = builder.get();
                break;
            case POST:
                builder = builder.post(RequestBody.create(
                        MediaType.parse(request.headers.getContentType()),
                        request.getBody()
                ));
        }

        // Populate headers
        Headers headers = new Headers();
        headers.putAll(request.headers);
        headers.remove(Headers.CONTENT_TYPE); // Already set in builder

        for (String name : headers.keySet()) {
            List<String> header = headers.get(name);

            if (header == null) continue;

            for (String value : header) {
                builder.addHeader(name, value);
            }
        }

        // Upgrade-Insecure-Requests
        if (request.getUrl().contains("http://")) {
            builder.addHeader(Headers.UPGRADE_INSECURE_REQUESTS, "1");
        }

        // Accept
        String accept = Client.acceptByExtension(request.getUrl());
        if (!accept.isEmpty()) {
            builder.addHeader(Headers.ACCEPT, accept);
        }

        if (context != null && context.getApplicationContext() != null) {
            wifi.bindToWifi();
        }

        last_call = client.newCall(builder.build());
        return new HttpResponse(request, last_call.execute());
    }

    @Override
    public void stop() {
        if (last_call != null) {
            last_call.cancel();
        }
    }

    private class InterceptedCookieJar implements CookieJar {
        private final CookieManager manager;
        private final CookieSyncManager syncmanager;

        public InterceptedCookieJar() {
            manager = CookieManager.getInstance();
            syncmanager = CookieSyncManager.createInstance(context);

        }

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            syncmanager.startSync();

            for (Cookie cookie : cookies) {
                manager.setCookie(url.toString(), cookie.toString());
            }

            syncmanager.stopSync();
            syncmanager.sync();
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            String rawCookies = manager.getCookie(url.toString());

            if (rawCookies == null) {
                return new LinkedList<>();
            }

            String[] rawCookiesList = rawCookies.split("; ");
            List<Cookie> result = new LinkedList<>();

            for (String cookie : rawCookiesList) {
                result.add(Cookie.parse(url, cookie));
            }

            return result;
        }
    }
}
