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

import org.jsoup.nodes.Element;

import java.text.ParseException;
import java.util.HashMap;

public class HtmlForm extends HashMap<String,String> {
    private Client.METHOD method = Client.METHOD.GET;
    private String action;

    public HtmlForm() {
        action = "";
    }

    public HtmlForm(String location, Element form) throws ParseException {
        if (form == null) {
            throw new ParseException("Form is null", 0);
        }

        action = HttpResponse.absolutePathToUrl(location, form.attr("action"));

        if ("post".equalsIgnoreCase(form.attr("method"))) {
            method = Client.METHOD.POST;
        }

        for (Element input : form.getElementsByTag("input")) {
            String value = input.attr("value");
            put(input.attr("name"), value != null ? value : "");
        }
    }

    public String getAction() {
        return action;
    }

    public HtmlForm setAction(String action) {
        this.action = action;
        return this;
    }

    public Client.METHOD getMethod() {
        return method;
    }

    public HtmlForm setMethod(Client.METHOD method) {
        this.method = method;
        return this;
    }
}
