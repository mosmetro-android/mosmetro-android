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

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.Option;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Response;
import okhttp3.ResponseBody;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Util;

public class HttpResponse {
    public static final LinkedList<String> PARSED_TYPES = new LinkedList<String>() {{
        add("text/html");
        add("text/plain");
        add("text/xml");

        add("application/x-www-form-urlencoded");
        add("application/json");
        add("application/xml");
        add("application/xhtml+xml");
    }};

    public final Headers headers = new Headers();

    private HttpRequest request;
    private InputStream stream;
    private int code;
    private String reason;

    private String body;
    private Document document;

    public static HttpResponse EMPTY(Client client) {
        return new HttpResponse(new HttpRequest(client, Client.METHOD.GET, ""), "");
    }

    public HttpResponse(@NonNull HttpRequest request, @NonNull Response response) throws IOException {
        this.request = request;
        this.code = response.code();
        this.reason = response.message();

        ResponseBody body = response.body();
        if (body == null) {
            throw new IOException("Response body is null! Code: " + code);
        }

        this.headers.putAll(response.headers().toMultimap());

        if (PARSED_TYPES.contains(this.headers.getMimeType())) {
            this.body = body.string();

            if (!this.body.isEmpty() && this.headers.getMimeType().contains("text/html")) {
                document = Jsoup.parse(this.body, getUrl());
            }
        } else {
            this.stream = body.byteStream();
        }
    }

    public HttpResponse(@NonNull HttpRequest request, @NonNull String body, int code, String reason,
                        @Nullable Headers headers) {
        this.request = request;
        this.code = code;
        this.reason = reason;

        if (headers != null){
            this.headers.putAll(headers);
        }

        this.body = body;

        if (!body.isEmpty() && this.headers.getMimeType().contains("text/html")) {
            document = Jsoup.parse(body, getUrl());
        }
    }

    public HttpResponse(HttpRequest request, String content, String contentType) {
        this(request, content, 200, "OK", new Headers() {{
            setHeader(Headers.CONTENT_TYPE, contentType);
            setHeader(Headers.ACAO, "*");
        }});
    }

    public HttpResponse(HttpRequest request, String body) {
        this(request, body, "text/html; charset=utf-8");
    }

    @NonNull
    public String getPage() {
        return body != null ? body : "";
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

    @Nullable
    public InputStream getInputStream() {
        if (stream != null) {
            return stream;
        }

        if (document != null) {
            return new ByteArrayInputStream(document.toString().getBytes());
        }

        if (body != null) {
            return new ByteArrayInputStream(body.getBytes());
        }

        return null;
    }

    public boolean isStream() {
        return stream != null;
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

    private final Pattern META_REFRESH = Pattern.compile("^[0-9]+[;,] ?(URL=|url=)?['\"]?(.*?)['\"]?$");

    @NonNull
    public String parseMetaRedirect() throws ParseException {
        String attr = parseMetaContent("refresh");
        Matcher matcher = META_REFRESH.matcher(attr);

        if (!matcher.matches()) {
            throw new ParseException("Meta redirect not found", 0);
        }

        String link = matcher.group(2);

        if (link == null || link.isEmpty()) {
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
        return (JSONObject) new JSONParser().parse(getPage());
    }

    @NonNull
    public DocumentContext jsonpath() {
        try {
            return Util.jsonpath(getPage());
        } catch (ParseException ex) {
            Logger.log(Logger.LEVEL.DEBUG, ex);
            return Util.JSONPATH_EMPTY;
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
        Map<String,String> result = new LinkedHashMap<>();

        for (Element input : form.getElementsByTag("input")) {
            String value = input.attr("value");

            if (value != null && !value.isEmpty())
                result.put(input.attr("name"), value);
        }

        return result;
    }

    public String toHeaderString() {
        StringBuilder builder = new StringBuilder();

        builder.append("URL: ").append(request.getUrl()).append("\n");
        builder.append(code).append(' ').append(reason).append("\n");

        for (String name : headers.keySet()) {
            List<String> header = headers.get(name);

            if (header == null) continue;

            for (String value : header) {
                builder.append(name).append(": ").append(value).append("\n");
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
