package pw.thedrhax.mosmetro.httpclient.clients;

import okhttp3.*;
import org.jsoup.Jsoup;
import pw.thedrhax.mosmetro.httpclient.Client;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OkHttp extends Client {
    private OkHttpClient client;

    private String referer = "http://curlmyip.org";

    public OkHttp() {
        client = new OkHttpClient.Builder()
                // Don't verify the hostname
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                })
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

        for (Map.Entry<String,String> entry : params.entrySet()) {
            body.add(entry.getKey(), entry.getValue());
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
        body.close();

        if (raw_document == null || raw_document.isEmpty()) {
            throw new Exception("Empty response");
        }

        document = Jsoup.parse(raw_document);

        // Clean-up useless tags: <script>, <style>
        document.getElementsByTag("script").remove();
        document.getElementsByTag("style").remove();
    }
}
