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

package pw.thedrhax.mosmetro.authenticator.providers;

import android.content.Context;
import android.net.Uri;

import org.jsoup.nodes.Element;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.InitialConnectionCheckTask;
import pw.thedrhax.mosmetro.authenticator.NamedTask;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.authenticator.WaitTask;
import pw.thedrhax.mosmetro.httpclient.ParsedResponse;
import pw.thedrhax.util.Logger;

/**
 * The MosMetroV2mcc class implements support for mcc_rm segment of MosMetroV2 algorithm.
 *
 * Detection: Location redirect contains "/www/login.chi" and body has "<h2>Browser error!</h2>".
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see Provider
 */

public class MosMetroV2mcc extends Provider {
    private String redirect;

    public MosMetroV2mcc(Context context, final ParsedResponse res) {
        super(context);

        /**
         * Checking Internet connection
         * ⇒ GET generate_204 < res
         * ⇐ Location redirect: http://10.x.x.x/www/login.chi?... > redirect
         */
        add(new InitialConnectionCheckTask(this, res) {
            @Override
            public boolean handle_response(HashMap<String, Object> vars, ParsedResponse response) {
                try {
                    redirect = response.parseAnyRedirect();
                } catch (ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_redirect)
                    ));
                    return false;
                }
                return true;
            }
        });

        /**
         * Follow all 300-redirects until first Provider is matched > provider
         */
        add(new WaitTask(this, context.getString(R.string.auth_redirect)) {
            private Provider provider = MosMetroV2mcc.this;

            @Override
            public boolean run(HashMap<String, Object> vars) {
                client.followRedirects(false);

                boolean result = super.run(vars);

                client.followRedirects(true);
                vars.put("provider", provider);

                return result;
            }

            @Override
            public boolean until(HashMap<String, Object> vars) {
                try {
                    ParsedResponse response = client.get(redirect, null, pref_retry_count);

                    redirect = response.get300Redirect();
                    Logger.log(Logger.LEVEL.DEBUG, response.toString());

                    provider = Provider.find(context, response);
                } catch (IOException|ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_server)
                    ));

                    stop();
                    return false;
                }

                return !(provider instanceof Unknown || provider instanceof MosMetroV2mcc);
            }
        }.tries(5));

        /**
         * Switching to detected Provider (most probably MosMetroV2(WV)) < provider
         */
        add(new NamedTask(context.getString(R.string.auth_checking_connection)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                if (!vars.containsKey("provider") || !(vars.get("provider") instanceof Provider)) {
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_provider)
                    ));
                    return false;
                }

                Provider provider = (Provider)vars.get("provider");
                vars.put("switch", provider.getName());

                Logger.log(context.getString(R.string.auth_algorithm_switch, provider.getName()));
                addAll(indexOf(this) + 1, provider.setClient(client).setCallback(callback));
                return true;
            }
        });
    }

    /**
     * Checks if current network is supported by this Provider implementation.
     * @param response  Instance of ParsedResponse.
     * @return          True if response matches this Provider implementation.
     */
    public static boolean match(ParsedResponse response) {
        Element el = response.getPageContent().getElementsByTag("h2").first();

        try {
            if (response.get300Redirect().contains("/www/login.chi"))
                if (el != null && "Browser error!".equals(el.html()))
                    return true;
        } catch (ParseException ignored) {}

        return false;
    }
}
