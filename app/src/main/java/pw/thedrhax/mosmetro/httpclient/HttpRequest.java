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
import java.util.List;

public class HttpRequest {
    private final Client client;

    public final Headers headers = new Headers();

    private Client.METHOD method;
    private Uri url;
    private String body;
    private int tries = 1;

    public HttpRequest(Client client, Client.METHOD method, String url) {
        this.client = client;
        this.method = method;
        this.url = Uri.parse(url);
        this.headers.putAll(client.headers);
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

    public HttpRequest setBody(String body, String contentType) {
        this.body = body;
        headers.setHeader(Headers.CONTENT_TYPE, contentType);
        return this;
    }

    public HttpRequest setBody(String body) {
        this.body = body;
        headers.setHeader(Headers.CONTENT_TYPE, "text/plain");
        return this;
    }

    public String getBody() {
        return body;
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
            List<String> header = headers.get(name);

            if (header == null) continue;

            for (String value : header) {
                result.append(name).append(": ").append(value).append('\n');
            }
        }

        if (body != null) {
            result.append("content-type: ").append(headers.getContentType()).append('\n');
            result.append('\n').append(body);
        }

        return result.toString();
    }

    public HttpResponse execute() throws IOException {
        return client.execute(this);
    }
}
