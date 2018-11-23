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

import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.InitialConnectionCheckTask;
import pw.thedrhax.mosmetro.authenticator.NamedTask;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.authenticator.Task;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.ParsedResponse;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Logger;

/**
 * The MosMetroV1 class supports the older version of the MosMetro algorithm.
 *
 * Detection: Meta or Location redirect contains "login.wi-fi.ru".
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see Provider
 */

public class MosMetroV1 extends Provider {
    protected String redirect;

    public MosMetroV1(final Context context, final ParsedResponse res) {
        super(context);

        /**
         * Checking Internet connection
         * ⇒ GET http://wi-fi.ru
         * ⇐ Meta-redirect: http://login.wi-fi.ru/am/UI/Login?... > redirect
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
         * Getting auth page
         * ⇒ GET http://login.wi-fi.ru/am/UI/Login?... < redirect
         * ⇐ Form: method="post" action="" > form
         * If there are two forms, registration is required.
         */
        add(new NamedTask(context.getString(R.string.auth_auth_page)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                ParsedResponse response;

                try {
                    response = client.get(redirect, null, pref_retry_count);
                    Logger.log(Logger.LEVEL.DEBUG, response.getPageContent().outerHtml());
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_auth_page)
                    ));
                    return false;
                }

                Elements forms = response.getPageContent().getElementsByTag("form");
                if (forms.size() > 1) {
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_not_registered)
                    ));
                    vars.put("result", RESULT.NOT_REGISTERED);
                    return false;
                }
                vars.put("form", ParsedResponse.parseForm(forms.first()));
                return true;
            }
        });

        /**
         * Sending login form
         * ⇒ POST http://login.wi-fi.ru/am/UI/Login?... < redirect, form
         */
        add(new NamedTask(context.getString(R.string.auth_auth_form)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                try {
                    HashMap<String,String> form = (HashMap<String,String>)vars.get("form");
                    client.post(redirect, form, pref_retry_count);
                    return true;
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_server)
                    ));
                    return false;
                }
            }
        });

        /**
         * Checking Internet connection
         */
        add(new NamedTask(context.getString(R.string.auth_checking_connection)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                if (isConnected()) {
                    Logger.log(context.getString(R.string.auth_connected));
                    vars.put("result", RESULT.CONNECTED);
                    return true;
                } else {
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_connection)
                    ));
                    return false;
                }
            }
        });
    }

    /**
     * Checks if current network is supported by this Provider implementation.
     * @param response  Instance of ParsedResponse.
     * @return          True if response matches this Provider implementation.
     */
    public static boolean match(ParsedResponse response) {
        try {
            return response.parseAnyRedirect().contains("login.wi-fi.ru");
        } catch (ParseException ex) {
            return false;
        }
    }
}
