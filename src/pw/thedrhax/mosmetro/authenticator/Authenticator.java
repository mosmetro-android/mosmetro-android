package pw.thedrhax.mosmetro.authenticator;

import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pw.thedrhax.mosmetro.authenticator.networks.MosMetro;
import pw.thedrhax.util.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class Authenticator {
    public static final String SSID = "";
    public static final String[] SUPPORTED_NETWORKS = new String[] {MosMetro.SSID};

    // Result state
    public static final int STATUS_CONNECTED = 0;
    public static final int STATUS_ALREADY_CONNECTED = 1;
    public static final int STATUS_NOT_REGISTERED = 2;
    public static final int STATUS_ERROR = 3;

    // Network check state
    public static final int CHECK_CONNECTED = 0;
    public static final int CHECK_WRONG_NETWORK = 1;
    public static final int CHECK_NOT_CONNECTED = 2;

	protected Logger logger;
    protected OkHttpClient client;
    protected String referer = "http://curlmyip.org";

    public Authenticator () {
        logger = new Logger();
        client = new OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                })
                .cookieJar(new CookieJar() {
                    private HashMap<HttpUrl, List<Cookie>> cookies = new HashMap<HttpUrl, List<Cookie>>();

                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        this.cookies.put(url, cookies);
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        List<Cookie> url_cookies = cookies.get(url);
                        return (url_cookies != null) ? url_cookies : new ArrayList<Cookie>();
                    }
                })
                .build();
    }

    public int start() {
        return connect();
    }

    public String getSSID() {
        return SSID;
    }

    /*
     * Logging
     */

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public Logger getLogger() {
        return logger;
    }

    /*
     * Response parsing
     */

    protected static String parseMetaRedirect (Document document) throws Exception {
        String link = null;

        for (Element element : document.getElementsByTag("meta")) {
            if (element.attr("http-equiv").equalsIgnoreCase("refresh")) {
                String attr = element.attr("content");
                link = attr.substring(attr.indexOf("=") + 1);
            }
        }

        if (link == null || link.isEmpty())
            throw new Exception ("Перенаправление не найдено");

        // Check protocol of the URL
        if (!(link.contains("http://") || link.contains("https://")))
            link = "http://" + link;

        return link;
    }

    protected Document getPageContent (String link, RequestBody params) throws Exception {
        Document document;

        // Get and parse the page
        Request.Builder request = new Request.Builder()
                .url(link).addHeader("Referer", referer);

        if (params == null) {
            request = request.get();
        } else {
            request = request.post(params);
        }

        ResponseBody body = client.newCall(request.build())
                .execute().body();
        String content = body.string();
        body.close();

        referer = link;

        if (content == null || content.isEmpty()) {
            throw new Exception("Страница не получена");
        }
        document = Jsoup.parse(content);

        // Clean-up useless tags: <script>, <style>
        document.getElementsByTag("script").remove();
        document.getElementsByTag("style").remove();

        return document;
    }

    protected static RequestBody parseForm (Element form) throws Exception {
        Elements inputs = form.getElementsByTag("input");
        FormBody.Builder result = new FormBody.Builder();
        
        for (Element input : inputs)
             result.add(input.attr("name"), input.attr("value"));

        return result.build();
    }

    /*
     * Connection sequence
     */

    public abstract int isConnected();

    protected abstract int connect();

    /*
     * Progress reporting
     */

    protected ProgressListener progressListener = new ProgressListener() {
        @Override
        public void onProgressUpdate(int progress) {

        }
    };

    public void setProgressListener (ProgressListener listener) {
        progressListener = listener;
    }

    public interface ProgressListener {
        void onProgressUpdate(int progress);
    }
}
