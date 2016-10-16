package pw.thedrhax.mosmetro.httpclient.clients;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import pw.thedrhax.mosmetro.httpclient.Client;

import java.util.HashMap;
import java.util.Map;

public class JsoupClient extends Client {
    private boolean followRedirects = true;
    private Map<String,String> cookies;

    public JsoupClient() {
        cookies = new HashMap<String,String>();
    }

    @Override
    public Client followRedirects(boolean follow) {
        this.followRedirects = follow; return this;
    }

    @Override
    public Client get(String link, Map<String, String> params) throws Exception {
        Connection.Response response = Jsoup
                .connect(link + requestToString(params))
                .cookies(cookies)
                .header("User-Agent", user_agent)
                .followRedirects(followRedirects)
                .method(Connection.Method.GET)
                .execute();

        cookies.putAll(response.cookies());
        referer = response.url().toString();
        parseDocument(response);

        return this;
    }

    @Override
    public Client post(String link, Map<String, String> params) throws Exception {
        Connection.Response response = Jsoup
                .connect(link)
                .data(params)
                .cookies(cookies)
                .header("User-Agent", user_agent)
                .followRedirects(followRedirects)
                .method(Connection.Method.POST)
                .execute();

        cookies.putAll(response.cookies());
        referer = response.url().toString();
        parseDocument(response);

        return this;
    }

    private void parseDocument (Connection.Response response) throws Exception {
        raw_document = response.body();

        if (raw_document == null || raw_document.isEmpty()) {
            throw new Exception("Empty response");
        }

        document = response.parse();

        // Clean-up useless tags: <script>, <style>
        document.getElementsByTag("script").remove();
        document.getElementsByTag("style").remove();
    }
}
