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

import android.os.SystemClock;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public abstract class Client {
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_REFERER = "Referer";
    public static final String HEADER_CSRF = "X-CSRF-Token";

    protected Document document;
    protected Map<String,String> headers;
    protected String raw_document;
    protected int code = 200;

    protected Client() {
        headers = new HashMap<>();

        String ua = System.getProperty("http.agent");
        setHeader(HEADER_USER_AGENT,
                "Mozilla/5.0 " + ua.substring(ua.indexOf("("), ua.indexOf(")") + 1) +
                " AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.85 Mobile Safari/537.36"
        );
        setHeader(HEADER_ACCEPT,
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
        );
    }

    // Settings methods
    public abstract Client followRedirects(boolean follow);

    public Client setHeader (String name, String value) {
        headers.put(name, value); return this;
    }

    public String getHeader (String name) {
        return headers.containsKey(name) ? headers.get(name) : null;
    }

    public Client resetHeaders () {
        headers = new HashMap<>();
        return this;
    }

    public abstract Client setCookie(String url, String name, String value);
    public abstract Map<String,String> getCookies(String url);

    public abstract Client setTimeout(int ms);

    // IO methods
    public abstract Client get(String link, Map<String,String> params) throws Exception;
    public abstract Client post(String link, Map<String,String> params) throws Exception;
    public abstract InputStream getInputStream(String link) throws Exception;

    // Retry methods
    private abstract class RetryOnException<T> {
        T run(int retries) throws Exception {
            Exception last_ex = null;
            for (int i = 0; i < retries; i++) {
                try {
                    return body();
                } catch (Exception ex) {
                    last_ex = ex;
                    SystemClock.sleep(1000);
                }
            }
            throw last_ex;
        }
        public abstract T body() throws Exception;
    }

    public Client get(final String link, final Map<String,String> params,
                      int retries) throws Exception {
        return new RetryOnException<Client>() {
            @Override
            public Client body() throws Exception {
                return get(link, params);
            }
        }.run(retries);
    }
    public Client post(final String link, final Map<String,String> params,
                       int retries) throws Exception {
        return new RetryOnException<Client>() {
            @Override
            public Client body() throws Exception {
                return post(link, params);
            }
        }.run(retries);
    }
    public InputStream getInputStream(final String link, int retries) throws Exception {
        return new RetryOnException<InputStream>() {
            @Override
            public InputStream body() throws Exception {
                return getInputStream(link);
            }
        }.run(retries);
    }

    // Parse methods
    public Document getPageContent() {
        return document;
    }

    public String getPage() {
        return raw_document;
    }

    public int getResponseCode() {
        return code;
    }

    public String parseMetaContent (String name) throws Exception {
        String value = null;

        for (Element element : document.getElementsByTag("meta")) {
            if (name.equalsIgnoreCase(element.attr("name")) ||
                    name.equalsIgnoreCase(element.attr("http-equiv"))) {
                value = element.attr("content");
            }
        }

        if (value == null || value.isEmpty())
            throw new Exception("Meta tag not found");

        return value;
    }

    public String parseMetaRedirect() throws Exception {
        String attr = parseMetaContent("refresh");
        String link = attr.substring(
                attr.indexOf(
                        attr.toLowerCase().contains("; url=") ? "=" : ";"
                ) + 1
        );

        if (link.isEmpty())
            throw new Exception("Meta redirect not found");

        // Check protocol of the URL
        if (!(link.contains("http://") || link.contains("https://")))
            link = "http://" + link;

        return link;
    }

    public static Map<String,String> parseForm (Element form) {
        Map<String,String> result = new HashMap<>();

        for (Element input : form.getElementsByTag("input")) {
            String value = input.attr("value");

            if (value != null && !value.isEmpty())
                result.put(input.attr("name"), value);
        }

        return result;
    }

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
}
