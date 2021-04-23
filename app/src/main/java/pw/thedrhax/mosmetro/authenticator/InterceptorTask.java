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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Pattern;

import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.HttpRequest;
import pw.thedrhax.mosmetro.httpclient.HttpResponse;

public abstract class InterceptorTask implements Task {
    private final Pattern pattern;

    protected HashMap<String,Object> vars = null;

    public InterceptorTask(String regex) {
        this(Pattern.compile(regex));
    }

    public InterceptorTask(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean run(HashMap<String, Object> vars) {
        this.vars = vars;
        return true;
    }

    @Nullable
    public HttpResponse request(Client client, HttpRequest request) throws IOException {
        return null;
    }

    @NonNull
    public HttpResponse response(Client client, HttpRequest request, HttpResponse response) throws IOException {
        return response;
    }

    @Override
    public String toString() {
        return "InterceptorTask{" + pattern.toString() + '}';
    }

    public boolean match(String url) {
        return pattern != null && pattern.matcher(url).matches();
    }
}
