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
    private static final int METHOD_GET = 0;
    private static final int METHOD_POST = 1;

    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_REFERER = "Referer";
    public static final String HEADER_CSRF = "X-CSRF-Token";

    protected Document document;
    protected Map<String,String> headers;
    protected String raw_document;
    protected int code = 200;

    protected Client() {
        headers = new HashMap<String, String>();

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
        headers = new HashMap<String, String>();
        return this;
    }

    public abstract Client setCookie(String url, String name, String value);

    // IO methods
    public abstract Client get(String link, Map<String,String> params) throws Exception;
    public abstract Client post(String link, Map<String,String> params) throws Exception;
    public abstract InputStream getInputStream(String link) throws Exception;

    public Client get(String link, Map<String,String> params, int retries) throws Exception {
        return requestWithRetries(link, params, retries, METHOD_GET);
    }
    public Client post(String link, Map<String,String> params, int retries) throws Exception {
        return requestWithRetries(link, params, retries, METHOD_POST);
    }
    public InputStream getInputStream(String link, int retries) throws Exception {
        Exception last_ex = null;
        for (int i = 0; i < retries; i++) {
            try {
                return getInputStream(link);
            } catch (Exception ex) {
                last_ex = ex;
                SystemClock.sleep(1000);
            }
        }
        throw last_ex;
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
                return this;
            } catch (Exception ex) {
                last_ex = ex;
                SystemClock.sleep(1000);
            }
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

    public int getResponseCode() {
        return code;
    }

    public String parseLinkRedirect() throws Exception {
        String link = document.getElementsByTag("a").first().attr("href");

        if (link == null || link.isEmpty())
            throw new Exception ("Link not found");

        return link;
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
            throw new Exception ("Meta tag not found");

        return value;
    }

    public String parseMetaRedirect() throws Exception {
        String link = null;

        String attr = parseMetaContent("refresh");
        if (attr.toLowerCase().contains("; url=")) {
            link = attr.substring(attr.indexOf("=") + 1);
        } else {
            link = attr.substring(attr.indexOf(";") + 1);
        }

        if (link == null || link.isEmpty())
            throw new Exception ("Meta redirect not found");

        // Check protocol of the URL
        if (!(link.contains("http://") || link.contains("https://")))
            link = "http://" + link;

        return link;
    }

    public static Map<String,String> parseForm (Element form) {
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
