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

import androidx.annotation.Nullable;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.providers.Unknown;
import pw.thedrhax.mosmetro.httpclient.HttpRequest;
import pw.thedrhax.mosmetro.httpclient.HttpResponse;
import pw.thedrhax.util.Logger;

public abstract class FollowRedirectsTask extends NamedTask {
    private final Provider p;
    private boolean ignoreErrors = false;
    private boolean switchProviders = false;

    public FollowRedirectsTask(Provider p) {
        super(p.context.getString(R.string.auth_redirect));
        this.p = p;
    }

    @Nullable
    public abstract String getInitialRedirect(HashMap<String, Object> vars);

    public abstract void getLastRedirect(String url);

    @Override
    public boolean run(HashMap<String, Object> vars) {
        String redirect = getInitialRedirect(vars);
        if (redirect == null) return true;

        p.client.setFollowRedirects(false);

        try {
            for (int i = 1; i < 10; i++) {
                HttpRequest req = p.client.get(redirect).retry();
                Logger.log(Logger.LEVEL.DEBUG, req.toString());

                HttpResponse res = req.execute(); // throws IOException
                Logger.log(Logger.LEVEL.DEBUG, res.toString());

                redirect = res.parseAnyRedirect(); // throws ParseException

                if (switchProviders) {
                    Provider nested = Provider.find(p.context, res);

                    if (!(nested instanceof Unknown)) {
                        p.add(p.indexOf(this) + 1, nested);
                        getLastRedirect(redirect);
                        return true;
                    }
                }
            }

            throw new IOException("Too many redirects");
        } catch (IOException ex) {
            Logger.log(Logger.LEVEL.DEBUG, ex);
            return ignoreErrors;
        } catch (ParseException ex) {
            Logger.log(Logger.LEVEL.DEBUG, ex);
        } finally {
            p.client.setFollowRedirects(true);
        }

        getLastRedirect(redirect);
        return true;
    }

    public FollowRedirectsTask setIgnoreErrors(boolean ignore) {
        this.ignoreErrors = ignore;
        return this;
    }

    public FollowRedirectsTask setSwitchProviders(boolean enabled) {
        this.switchProviders = enabled;
        return this;
    }
}
