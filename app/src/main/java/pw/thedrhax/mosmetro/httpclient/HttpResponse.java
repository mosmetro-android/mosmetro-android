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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HttpResponse {
    public final Headers headers = new Headers();

    private HttpRequest request;
    private byte[] bytes;
    private int code;
    private String reason;

    private String html;
    private Document document;

    public static HttpResponse EMPTY(Client client) {
        return new HttpResponse(new HttpRequest(client, Client.METHOD.GET, ""), "");
    }

    public HttpResponse(@NonNull HttpRequest request, @NonNull byte[] bytes, int code, String reason,
                        @Nullable Headers headers) {
        this.request = request;
        this.bytes = bytes;
        this.code = code;
        this.reason = reason;

        if (headers != null){
            this.headers.putAll(headers);
        }

        try {
            body = new String(bytes, headers.getEncoding());
        } catch (UnsupportedEncodingException ex) {
            Logger.log(Logger.LEVEL.DEBUG, ex);
        }

        if (body != null && !body.isEmpty() && headers.getMimeType().contains("text/html")) {
            document = Jsoup.parse(body, getUrl());
        }
    }

    public HttpResponse(HttpRequest request, String content, String contentType) {
        this(request, content.getBytes(), 200, "OK", new Headers() {{
            setHeader(Headers.CONTENT_TYPE, contentType);
            setHeader(Headers.ACAO, "*");
        }});
    }

    public HttpResponse(HttpRequest request, String html) {
        this(request, html, "text/html; charset=utf-8");
    }

    @NonNull
    public String getPage() {
        return html;
    }

    @NonNull
    public String getUrl() {
        return request.getUrl();
    }

    public int getResponseCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }

    public boolean isHtml() {
        return document != null;
    }

    public Document getPageContent() {
        return document != null ? document : Jsoup.parse("<html></html>");
    }

    @NonNull
    public byte[] getBytes() {
        if (document != null) {
            return document.outerHtml().getBytes();
        } else {
            return bytes;
        }
    }

    @Nullable
    public InputStream getInputStream() {
        return new ByteArrayInputStream(getBytes());
    }

    public HttpRequest getRequest() {
        return request;
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

    @NonNull
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

        // Workaround for auth.wi-fi.ru[/]?segment=
        if (link.contains("?"))
            if (!link.substring(link.indexOf("://") + 3, link.indexOf("?")).contains("/"))
                link = link.replace("?", "/?");

        return absolutePathToUrl(getUrl(), link);
    }

    @NonNull
    public String parseAnyRedirect() throws ParseException {
        try {
            return parseMetaRedirect();
        } catch (ParseException ex1) {
            return get300Redirect();
        }
    }

    @NonNull
    public JSONObject json() throws org.json.simple.parser.ParseException {
        if (html != null) {
            return (JSONObject) new JSONParser().parse(getPage());
        } else {
            throw new org.json.simple.parser.ParseException(0);
        }
    }

    @NonNull
    public String get300Redirect() throws ParseException {
        String link = headers.getFirst(Headers.LOCATION);

        if (link == null || link.isEmpty()) {
            throw new ParseException("Location header is not present", 0);
        }

        // Workaround for auth.wi-fi.ru[/]?segment=
        if (link.contains("?"))
            if (!link.substring(link.indexOf("://") + 3, link.indexOf("?")).contains("/"))
                link = link.replace("?", "/?");

        return absolutePathToUrl(getUrl(), link);
    }

    public static String removePathFromUrl(String url) {
        Uri base_uri = Uri.parse(url);
        return base_uri.getScheme() + "://" + base_uri.getHost();
    }

    public static String absolutePathToUrl(String baseUrl, String path) throws ParseException {
        String base = removePathFromUrl(baseUrl);

        if (path.startsWith("//")) {
            return Uri.parse(baseUrl).getScheme() + ":" + path;
        } else if (path.startsWith("/")) {
            return base + path;
        } else if (path.startsWith("http")) {
            return path;
        } else {
            throw new ParseException("Malformed URL: " + path, 0);
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

    public String toHeaderString() {
        StringBuilder builder = new StringBuilder();

        builder.append("URL: ").append(" ").append(request.getUrl()).append("\n");
        builder.append(code).append(' ').append(reason).append("\n");

        for (String name : headers.keySet()) {
            List<String> header = headers.get(name);

            if (header == null) continue;

            for (String value : header) {
                builder.append(header).append(": ").append(value).append("\n");
            }
        }

        return builder.toString();
    }

    public String toBodyString() {
        if (document != null) {
            String html = document.outerHtml();
            if (html.length() <= 2000) {
                return html;
            } else {
                return "<!-- file is too long -->";
            }
        } else {
            try {
                return json().toJSONString();
            } catch (org.json.simple.parser.ParseException ignored) {}
        }

        return "<!-- format not supported -->";
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(toHeaderString()).append("\n");
        builder.append(toBodyString());
        return builder.toString();
    }
}
