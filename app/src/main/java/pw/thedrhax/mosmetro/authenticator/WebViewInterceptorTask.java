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

package pw.thedrhax.mosmetro.authenticator;

import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.regex.Pattern;

import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.ParsedResponse;
import pw.thedrhax.mosmetro.services.WebViewService;

public abstract class WebViewInterceptorTask implements Task {
    private Pattern pattern;

    public WebViewInterceptorTask(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    @Override
    public boolean run(HashMap<String, Object> vars) {
        return true;
    }

    @Nullable
    public abstract ParsedResponse request(WebViewService wv, Client client, String url);

    public boolean match(String url) {
        return pattern.matcher(url).matches();
    }
}
