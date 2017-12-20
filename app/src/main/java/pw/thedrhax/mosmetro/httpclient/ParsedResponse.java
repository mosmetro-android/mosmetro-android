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

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pw.thedrhax.util.Logger;

public class ParsedResponse {
    private String url;
    private String html;
    private Document document;
    private int code;
    private Map<String,List<String>> headers = new HashMap<>();

    public ParsedResponse(@Nullable String url, @Nullable String html, int code,
                          @Nullable Map<String,List<String>> headers) {
        this.url = url;
        this.html = html;

        if (html != null && !html.isEmpty()) {
            document = Jsoup.parse(html, url);

            // Clean-up useless tags: <script> without src, <style>
            for (Element element : document.getElementsByTag("script")) {
                if (!element.hasAttr("src")) {
                    element.remove();
                }
            }
            document.getElementsByTag("style").remove();
        }

        this.code = code;

        if (headers != null){
            this.headers.putAll(headers);
        }
    }

    public ParsedResponse(String html) {
        this("", html, 200, null);
    }

    @NonNull
    public String getPage() {
        return html;
    }

    @Nullable
    public String getURL() {
        return url;
    }

    public int getResponseCode() {
        return code;
    }

    @Nullable
    public String getResponseHeader(String name) {
        if (headers != null && headers.get(name.toLowerCase()) != null) {
            return headers.get(name.toLowerCase()).get(0);
        } else {
            return null;
        }
    }

    public Document getPageContent() {
        return document != null ? document : Jsoup.parse("<html></html>");
    }

    public String parseMetaContent (String name) throws ParseException {
        String value = null;

        if (document == null) {
            throw new ParseException("Document is null!", 0);
        }

        for (Element element : document.getElementsByTag("meta")) {
            if (name.equalsIgnoreCase(element.attr("name")) ||
                    name.equalsIgnoreCase(element.attr("http-equiv"))) {
                value = element.attr("content");
            }
        }

        if (value == null || value.isEmpty()) {
            throw new ParseException("Meta tag '" + name + "' not found", 0);
        }

        return value;
    }

    public String parseMetaRedirect() throws ParseException {
        String attr = parseMetaContent("refresh");
        String link = attr.substring(
                attr.indexOf(
                        attr.toLowerCase().contains("; url=") ? "=" : ";"
                ) + 1
        );

        if (link.isEmpty()) {
            throw new ParseException("Meta redirect not found", 0);
        }

        // Check protocol of the URL
        if (!(link.contains("http://") || link.contains("https://"))) {
            link = "http://" + link;
        }
        
        if (link.contains("?"))
            if (!link.substring(link.indexOf("://") + 3, link.indexOf("?")).contains("/"))
                link = link.replace("?", "/?");

        return link;
    }

    @NonNull
    public JSONObject json() throws org.json.simple.parser.ParseException {
        return (JSONObject) new JSONParser().parse(getPage());
    }

    @NonNull
    public String get300Redirect() throws ParseException {
        String redirect = getResponseHeader(Client.HEADER_LOCATION);

        if (redirect == null || redirect.isEmpty()) {
            throw new ParseException("302 redirect is empty", 0);
        } else {
            return redirect;
        }
    }

    public static String removePathFromUrl(String url) {
        Uri base_uri = Uri.parse(url);
        return base_uri.getScheme() + "://" + base_uri.getHost();
    }

    private static String absolutePathToUrl(String base_url, String path) throws ParseException {
        String base = removePathFromUrl(base_url);

        if (path.startsWith("//")) {
            return Uri.parse(base_url).getScheme() + ":" + path;
        } else if (path.startsWith("/")) {
            return base + path;
        } else if (path.startsWith("http")) {
            return path;
        } else {
            throw new ParseException("Malformed URL: " + path, 0);
        }
    }

    public List<String> parseResourceList() {
        LinkedList<String> links = new LinkedList<>();

        if (document == null) {
            return links;
        }

        // <link href="..." />
        for (Element element : document.getElementsByTag("link")) {
            if (element.hasAttr("href"))
                links.add(element.attr("href"));
        }

        // <script src="..." />
        for (Element element : document.getElementsByTag("script")) {
            if (element.hasAttr("src"))
                links.add(element.attr("src"));
        }

        // Absolute path to full URL
        LinkedList<String> result = new LinkedList<>();
        for (String link : links) {
            try {
                result.add(absolutePathToUrl(document.location(), link));
            } catch (ParseException ex) {
                Logger.log(Logger.LEVEL.DEBUG, ex);
            }
        }

        return result;
    }

    public void loadResources(Client client) {
        for (String link : parseResourceList()) {
            Logger.log(this, link);
            try {
                client.get(link, null);
            } catch (IOException ignored) {}
        }
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

    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("URL: ").append(" ").append(url).append("\n");
        builder.append("Response code: ").append(code).append("\n");

        for (String header : headers.keySet()) {
            for (String value : headers.get(header)) {
                builder.append(header).append(": ").append(value).append("\n");
            }
        }

        if (document != null) {
            builder.append(document.outerHtml());
        }

        return builder.toString();
    }
}
