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

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
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
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.util.WifiUtils;
import pw.thedrhax.mosmetro.httpclient.ParsedResponse;

public class OkHttp extends Client {
    private OkHttpClient client;
    private Call last_call = null;

    public OkHttp(Context context) {
        super(context);
        client = new OkHttpClient.Builder().cookieJar(new InterceptedCookieJar()).build();
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
                .connectTimeout(ms, TimeUnit.MILLISECONDS)
                .readTimeout(ms, TimeUnit.MILLISECONDS)
                .writeTimeout(ms, TimeUnit.MILLISECONDS)
                .build();

        return this;
    }

    @Override
    public Client followRedirects(boolean follow) {
        client = client.newBuilder()
                .followRedirects(follow)
                .followSslRedirects(follow)
                .build();

        return this;
    }

    private Response call(String url, RequestBody data) throws IOException {
        Request.Builder builder = new Request.Builder().url(url);

        // Choose appropriate request method
        if (data == null) {
            builder = builder.get();
        } else {
            builder = builder.post(data);
        }

        // Populate headers
        for (String name : headers.keySet()) {
            builder.addHeader(name, getHeader(name));
        }

        // Upgrade-Insecure-Requests
        if (url.contains("http://")) {
            builder.addHeader(Client.HEADER_UPGRADE_INSECURE_REQUESTS, "1");
        }

        // Accept
        String accept = Client.acceptByExtension(url);
        if (!accept.isEmpty()) {
            builder.addHeader(Client.HEADER_ACCEPT, accept);
        }

        if (context != null && context.getApplicationContext() != null) {
            new WifiUtils(context).bindToWifi();
        }

        last_call = client.newCall(builder.build());
        return last_call.execute();
    }

    @Override
    public ParsedResponse get(String link, Map<String, String> params) throws IOException {
        return parse(call(link + requestToString(params), null));
    }

    @Override
    public ParsedResponse post(String link, Map<String, String> params) throws IOException {
        FormBody.Builder body = new FormBody.Builder();

        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getValue() != null)
                    body.add(entry.getKey(), entry.getValue());
            }
        }

        return parse(call(link, body.build()));
    }

    @Override
    public ParsedResponse post(String link, String type, String body) throws IOException {
        return parse(call(link, RequestBody.create(MediaType.parse(type), body)));
    }

    @Override
    public InputStream getInputStream(String link) throws IOException {
        Response response = call(link, null);
        ResponseBody body = response.body();

        if (body == null) {
            throw new IOException("Empty response: " + response.code());
        }

        return body.byteStream();
    }

    @Override
    public void stop() {
        if (last_call != null) {
            last_call.cancel();
        }
    }

    private ParsedResponse parse(Response response) throws IOException {
        ResponseBody body = response.body();

        if (body == null) {
            throw new IOException("Response body is null! Code: " + response.code());
        }

        return new ParsedResponse(
                response.request().url().toString(), body.string(),
                response.code(), response.headers().toMultimap()
        );
    }

    private class InterceptedCookieJar implements CookieJar {
        private HashMap<HttpUrl, List<Cookie>> cookies = new HashMap<>();

        private HttpUrl getHost (HttpUrl url) {
            return HttpUrl.parse("http://" + url.host());
        }

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            HttpUrl host = getHost(url);
            List<Cookie> url_cookies = loadForRequest(host);
            for (Cookie cookie : cookies) {
                List<Cookie> for_deletion = new ArrayList<>();
                for (Cookie old_cookie : url_cookies) {
                    if (cookie.name().equals(old_cookie.name()))
                        for_deletion.add(old_cookie);
                }
                for (Cookie old_cookie : for_deletion) {
                    url_cookies.remove(old_cookie);
                }
                url_cookies.add(cookie);
            }
            this.cookies.put(host, url_cookies);
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            HttpUrl host = getHost(url);
            List<Cookie> url_cookies = cookies.get(host);
            return (url_cookies != null) ? url_cookies : new ArrayList<Cookie>();
        }
    }
}
