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

package pw.thedrhax.httpclient.clients;

import org.jsoup.Jsoup;

import java.io.InputStream;
import java.util.Map;

import pw.thedrhax.mosmetro.httpclient.Client;

public class DummyClient extends Client {

    public DummyClient(String response) {
        this.raw_document = response;
        this.document = Jsoup.parse(response);
        this.code = 200;
    }

    @Override
    public Client get(String link, Map<String, String> params) throws Exception {
        return this;
    }

    @Override
    public Client post(String link, Map<String, String> params) throws Exception {
        return this;
    }

    @Override
    public InputStream getInputStream(String link) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public Client followRedirects(boolean follow) {
        return this;
    }

    @Override
    public Client setCookie(String url, String name, String value) {
        return this;
    }
}