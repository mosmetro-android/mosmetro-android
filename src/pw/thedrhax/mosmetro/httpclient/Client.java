package pw.thedrhax.mosmetro.httpclient;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.HashMap;
import java.util.Map;

public abstract class Client {
    private static final int METHOD_GET = 0;
    private static final int METHOD_POST = 1;

    protected Document document;
    protected String user_agent = System.getProperty("http.agent");
    protected String raw_document;
    protected int code = 200;

    protected Client() {}

    // Settings methods
    public abstract Client followRedirects(boolean follow);

    public Client setUserAgent(String user_agent) {
        this.user_agent = user_agent; return this;
    }

    public abstract Client setCookie(String url, String name, String value);

    // IO methods
    public abstract Client get(String link, Map<String,String> params) throws Exception;
    public abstract Client post(String link, Map<String,String> params) throws Exception;

    public Client get(String link, Map<String,String> params, int retries) throws Exception {
        return requestWithRetries(link, params, retries, METHOD_GET);
    }
    public Client post(String link, Map<String,String> params, int retries) throws Exception {
        return requestWithRetries(link, params, retries, METHOD_POST);
    }

    private Client requestWithRetries(String link, Map<String,String> params,
                                      int retries, int method) throws Exception {
        Exception last_ex = null;
        for (int i = 0; i < retries; i++) {
            try {
                switch (method) {
                    case METHOD_GET: get(link, params); break;
                    case METHOD_POST: post(link, params); break;
                }
            } catch (Exception ex) {
                last_ex = ex;
                continue;
            }
            return this;
        }
        throw last_ex;
    }

    // Parse methods
    public Document getPageContent() {
        return document;
    }

    public String getPage() {
        return raw_document;
    }

    public abstract String getReferer();

    public int getResponseCode() {
        return code;
    }

    public String parseLinkRedirect() throws Exception {
        String link = document.getElementsByTag("a").first().attr("href");

        if (link == null || link.isEmpty())
            throw new Exception ("Link not found");

        return link;
    }

    public String parseMetaRedirect() throws Exception {
        String link = null;

        for (Element element : document.getElementsByTag("meta")) {
            if (element.attr("http-equiv").equalsIgnoreCase("refresh")) {
                String attr = element.attr("content");
                if (attr.toLowerCase().contains("; url=")) {
                    link = attr.substring(attr.indexOf("=") + 1);
                } else {
                    link = attr.substring(attr.indexOf(";") + 1);
                }
            }
        }

        if (link == null || link.isEmpty())
            throw new Exception ("Meta redirect not found");

        // Check protocol of the URL
        if (!(link.contains("http://") || link.contains("https://")))
            link = "http://" + link;

        return link;
    }

    public static Map<String,String> parseForm (Element form) throws Exception {
        Map<String,String> result = new HashMap<String,String>();

        for (Element input : form.getElementsByTag("input")) {
            String value = input.attr("value");

            if (value != null && !value.isEmpty())
                result.put(input.attr("name"), value);
        }

        return result;
    }

    // Convert methods
    protected static String requestToString (Map<String,String> params) {
        if (params == null) return "";

        StringBuilder params_string = new StringBuilder();

        for (Map.Entry<String,String> entry : params.entrySet()) {
            if (params_string.length() == 0) {
                params_string.append("?");
            } else {
                params_string.append("&");
            }

            params_string
                    .append(entry.getKey())
                    .append("=")
                    .append(entry.getValue());
        }

        return params_string.toString();
    }
}
