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

import android.content.Context;

import org.jsoup.Jsoup;

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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.util.Util;

public class OkHttp extends Client {
    private OkHttpClient client;
    private Call last_call = null;

    private SSLSocketFactory trustAllCerts() {
        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                }
        };

        // Install the all-trusting trust manager
        try {
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Create an ssl socket factory with our all-trusting manager
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException ignored) {
        } catch (KeyManagementException ignored) {}

        return null;
    }

    public OkHttp() {
        client = new OkHttpClient.Builder()
                // Don't verify the hostname
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                })
                .sslSocketFactory(trustAllCerts())
                // Store cookies for this session
                .cookieJar(new CookieJar() {
                    private HashMap<HttpUrl, List<Cookie>> cookies = new HashMap<HttpUrl, List<Cookie>>();

                    private HttpUrl getHost (HttpUrl url) {
                        return HttpUrl.parse("http://" + url.host());
                    }

                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        HttpUrl host = getHost(url);
                        List<Cookie> url_cookies = loadForRequest(host);
                        // TODO: You can do better, come on!
                        for (Cookie cookie : cookies) {
                            List<Cookie> for_deletion = new ArrayList<Cookie>();
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
                })
                .build();
    }

    public OkHttp(Context context) {
        this();
        int timeout = Util.getIntPreference(context, "pref_timeout", 5);
        if (timeout != 0) setTimeout(timeout * 1000);
    }

    @Override
    public Client setCookie(String url, String name, String value) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        List<Cookie> url_cookies = new ArrayList<Cookie>();
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

    private Response call(Request.Builder builder) throws IOException {
        // Populate headers
        for (String name : headers.keySet()) {
            builder.addHeader(name, getHeader(name));
        }

        last_call = client.newCall(builder.build());
        return last_call.execute();
    }

    @Override
    public Client get(String link, Map<String, String> params) throws Exception {
        parseDocument(call(
                new Request.Builder().url(link + requestToString(params)).get()
        ));
        setHeader(HEADER_REFERER, link);
        return this;
    }

    @Override
    public Client post(String link, Map<String, String> params) throws Exception {
        FormBody.Builder body = new FormBody.Builder();

        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                body.add(entry.getKey(), entry.getValue());
            }
        }

        parseDocument(call(
                new Request.Builder().url(link).post(body.build())
        ));
        setHeader(HEADER_REFERER, link);
        return this;
    }

    @Override
    public InputStream getInputStream(String link) throws Exception {
        Response response = call(
                new Request.Builder().url(link).get()
        );
        code = response.code();

        if (code != 200) {
            response.body().close();
            throw new Exception("Empty response: " + code);
        }

        return response.body().byteStream();
    }

    @Override
    public void stop() {
        if (last_call != null) {
            last_call.cancel();
        }
    }

    private void parseDocument (Response response) throws Exception {
        ResponseBody body = response.body();
        raw_document = body.string();
        code = response.code();
        body.close();

        if (raw_document == null || raw_document.isEmpty()) {
            if (code >= 400) {
                throw new Exception("Error: " + code);
            } else {
                return;
            }
        }

        document = Jsoup.parse(raw_document);

        // Clean-up useless tags: <script>, <style>
        document.getElementsByTag("script").remove();
        document.getElementsByTag("style").remove();
    }
}
