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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.ParsedResponse;
import pw.thedrhax.util.Logger;

public abstract class InterceptorTask implements Task {
    private Provider p;
    private String regex;
    private PatternSyntaxException ex = null;
    private Pattern pattern;

    public InterceptorTask(Provider p, String regex) {
        this.p = p;
        this.regex = regex;

        try {
            this.pattern = Pattern.compile(regex);
        } catch (PatternSyntaxException ex) {
            this.pattern = null;
            this.ex = ex;
        }
    }

    @Override
    public boolean run(HashMap<String, Object> vars) {
        if (ex != null) {
            Logger.log(Logger.LEVEL.DEBUG, ex);
            Logger.log(p.context.getString(R.string.error,
                    p.context.getString(R.string.auth_error_regex, regex)
            ));
            return false;
        }
        return true;
    }

    @Nullable
    public abstract ParsedResponse request(Client client, Client.METHOD method, String url, Map<String,String> params) throws IOException;

    @NonNull
    public abstract ParsedResponse response(Client client, String url, ParsedResponse response) throws IOException;

    public boolean match(String url) {
        return pattern != null && pattern.matcher(url).matches();
    }
}
