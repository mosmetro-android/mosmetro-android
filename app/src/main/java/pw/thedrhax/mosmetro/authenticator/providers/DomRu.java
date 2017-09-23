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

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.authenticator.Task;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.util.Logger;

/**
 * This class is used to provide automatic authentication
 * for DomRu provider in Saint Petersburg and other cities (DOM.RU Wi-Fi)
 *
 * Detection: 302 redirect contains domain "wifi.domru.ru"
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see Provider
 */

public class DomRu extends Provider {
    private String redirect;

    public DomRu(final Context context) {
        super(context);

        /**
         * Getting initial redirect
         * ⇒ generate_204()
         * ⇐ 302 redirect: https://spb.wifi.domru.ru/index.php?... > redirect
         */
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                Logger.log(context.getString(R.string.auth_checking_connection));

                client.followRedirects(false); // Workaround for 302 redirect

                if (isConnected()) {
                    Logger.log(context.getString(R.string.auth_already_connected));
                    vars.put("result", RESULT.ALREADY_CONNECTED);
                    return false;
                }

                try {
                    redirect = client.parse302Redirect();
                } catch (ParseException ex) {
                    redirect = "https://spb.wifi.domru.ru/index.php";
                }
                return true;
            }
        });

        /**
         * Getting auth page
         * ⇒ GET https://spb.wifi.domru.ru/index.php?... < redirect
         * ⇐ Link: /guest
         */
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                Logger.log(context.getString(R.string.auth_auth_page));

                try {
                    client.get(redirect, null, pref_retry_count);
                    Logger.log(Logger.LEVEL.DEBUG, client.getPageContent().outerHtml());
                    return true;
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_auth_page)
                    ));
                    return false;
                }
            }
        });

        /**
         * "Clicking" guest button
         * ⇒ GET https://spb.wifi.domru.ru/guest
         * ⇐ 302 redirect (on success)
         * ⇐ ??? (on failure)
         */
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                Logger.log(context.getString(R.string.auth_auth_form));

                Uri redirect_uri = Uri.parse(redirect);
                redirect = redirect_uri.getScheme() + "://" + redirect_uri.getHost();
                Logger.log(Logger.LEVEL.DEBUG, redirect);

                try {
                    client.get(redirect_uri + "/guest", null, pref_retry_count);
                    Logger.log(Logger.LEVEL.DEBUG, client.getPageContent().outerHtml());
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
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                Logger.log(context.getString(R.string.auth_checking_connection));

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
     * @param client    Client instance to get the information from. Provider.find()
     *                  will execute one request to be analyzed by this method.
     * @return          True if response matches this Provider implementation.
     */
    public static boolean match(Client client) {
        try {
            return client.parse302Redirect().contains("wifi.domru.ru");
        } catch (ParseException ex) {
            return false;
        }
    }
}
