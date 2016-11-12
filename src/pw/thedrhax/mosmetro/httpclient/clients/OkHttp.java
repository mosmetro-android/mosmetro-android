package pw.thedrhax.mosmetro.httpclient.clients;

import okhttp3.*;
import org.jsoup.Jsoup;
import pw.thedrhax.mosmetro.httpclient.Client;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import java.security.cert.CertificateException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OkHttp extends Client {
    private OkHttpClient client;

    private String referer = "http://curlmyip.org";

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

                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        List<Cookie> url_cookies = loadForRequest(url);
                        url_cookies.addAll(cookies);
                        this.cookies.put(url, url_cookies);
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        List<Cookie> url_cookies = cookies.get(url);
                        return (url_cookies != null) ? url_cookies : new ArrayList<Cookie>();
                    }
                })
                .build();
    }

    @Override
    public Client setCookie(String url, String name, String value) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        List<Cookie> url_cookies = client.cookieJar().loadForRequest(httpUrl);
        url_cookies.add(Cookie.parse(httpUrl, name + "=" + value));
        client.cookieJar().saveFromResponse(httpUrl, url_cookies);
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

    @Override
    public Client get(String link, Map<String, String> params) throws Exception {
        Request request = new Request.Builder()
                .url(link + requestToString(params))
                .addHeader("Referer", referer)
                .addHeader("User-Agent", user_agent)
                .get()
                .build();

        referer = link;
        parseDocument(client.newCall(request).execute());

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

        Request request = new Request.Builder()
                .url(link)
                .addHeader("Referer", referer)
                .addHeader("User-Agent", user_agent)
                .post(body.build())
                .build();

        referer = link;
        parseDocument(client.newCall(request).execute());

        return this;
    }

    private void parseDocument (Response response) throws Exception {
        ResponseBody body = response.body();
        raw_document = body.string();
        code = response.code();
        body.close();

        if (raw_document == null || raw_document.isEmpty()) {
            throw new Exception("Empty response: " + code);
        }

        document = Jsoup.parse(raw_document);

        // Clean-up useless tags: <script>, <style>
        document.getElementsByTag("script").remove();
        document.getElementsByTag("style").remove();
    }

    @Override
    public String getReferer() {
        return referer;
    }
}
