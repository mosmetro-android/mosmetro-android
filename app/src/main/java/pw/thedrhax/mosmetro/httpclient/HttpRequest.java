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

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HttpRequest {
    private final Client client;

    private Client.METHOD method;
    private Uri url;
    private String contentType;
    private String body;
    private Map<String, List<String>> headers = new HashMap<>();
    private int tries = 1;

    public HttpRequest(Client client, Client.METHOD method, String url) {
        this.client = client;
        this.method = method;
        this.url = Uri.parse(url);
    }

    public HttpRequest setMethod(Client.METHOD method) {
        this.method = method;
        return this;
    }

    public Client.METHOD getMethod() {
        return method;
    }

    public String getUrl() {
        return url.toString();
    }

    public HttpRequest setUrl(String url) {
        this.url = Uri.parse(url);
        return this;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public HttpRequest setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public HttpRequest setBody(String body, String contentType) {
        this.body = body;
        this.contentType = contentType;
        return this;
    }

    public HttpRequest setBody(String body) {
        this.body = body;
        this.contentType = "text/plain";
        return this;
    }

    public String getBody() {
        return body;
    }

    public String getContentType() {
        return contentType;
    }

    public HttpRequest setHeader(String name, String value) {
        headers.put(name.toLowerCase(), new LinkedList<String>() {{
            add(value);
        }});

        return this;
    }

    public List<String> getHeader(String name) {
        if (headers.containsKey(name.toLowerCase())) {
            return headers.get(name.toLowerCase());
        } else {
            return null;
        }
    }

    public HttpRequest setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
        return this;
    }

    public HttpRequest setTries(int tries) {
        this.tries = tries;
        return this;
    }

    public int getTries() {
        return tries;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append(method.toString()).append(' ').append(url.getPath()).append('\n');

        for (String name : headers.keySet()) {
            for (String value : headers.get(name)) {
                result.append(name).append(": ").append(value).append('\n');
            }
        }

        if (body != null) {
            result.append('\n').append(body);
        }

        return result.toString();
    }

    public HttpResponse execute() throws IOException {
        return client.execute(this);
    }
}
