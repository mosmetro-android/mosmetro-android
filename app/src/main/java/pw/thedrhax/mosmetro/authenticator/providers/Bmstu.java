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
import android.content.SharedPreferences;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.FinalConnectionCheckTask;
import pw.thedrhax.mosmetro.authenticator.InitialConnectionCheckTask;
import pw.thedrhax.mosmetro.authenticator.NamedTask;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.httpclient.HttpResponse;
import pw.thedrhax.util.Logger;

/**
 * The Bmstu class implements support for the public Wi-Fi network of BMSTU (bmstu_lb).
 *
 * Detection: Meta or Location redirect contains "lbpfs.bmstu.ru".
 *
 * @author Alexey Slokva <Alesha72003@ya.ru>
 * @see Provider
 */

public class Bmstu extends Provider {
    private static final String CREDS_KEY = "pref_bmstu_credentials";

    private String redirect = "https://lbpfs.bmstu.ru:8003/index.php?zone=bmstu_lb";

    public Bmstu(Context context, HttpResponse res) {
        super(context);

        /**
         * Checking Internet connection
         * ⇒ GET generate_204 < res
         * ⇐ Meta or Location redirect: https://lbpfs.bmstu.ru:8003/index.php?... > redirect
         */
        add(new InitialConnectionCheckTask(this, res) {
            @Override
            public boolean handle_response(HashMap<String, Object> vars, HttpResponse response) {
                try {
                    String redirect_fake = response.parseAnyRedirect();
                    Logger.log(Logger.LEVEL.DEBUG, redirect_fake);
                } catch (ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(Logger.LEVEL.DEBUG, "Redirect not found in response, using default");
                }

                return true;
            }
        });

        /**
         * Sending auth form
         * ⇒ POST https://lbpfs.bmstu.ru:8003/index.php < redirect
         * ⇐ ???
         */
        add(new NamedTask(context.getString(R.string.auth_auth_form)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                String login = settings.getString(CREDS_KEY + "_login", "");
                String password = settings.getString(CREDS_KEY + "_password", "");

                if (login.isEmpty() || password.isEmpty()) {
                    Logger.log(context.getString(R.string.error, 
                            context.getString(R.string.auth_error_bmstu_credentials)
                    ));
                    vars.put("result", RESULT.ERROR);
                    return false;
                }

                try {
                    HttpResponse response = client.post(redirect, new HashMap<String,String>() {{
                        put("redirurl", "");
                        put("auth_user", login);
                        put("auth_pass", password);
                        put("accept", "Continue");
                    }}).retry().execute();

                    Logger.log(Logger.LEVEL.DEBUG, response.toString());
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_server)
                    ));
                    return false;
                }

                return true;
            }
        });

        add(new FinalConnectionCheckTask(this));
    }

    /**
     * Checks if current network is supported by this Provider implementation.
     * @param response  Instance of ParsedResponse.
     * @return          True if response matches this Provider implementation.
     */
    public static boolean match(HttpResponse response) {
        try {
            return response.parseAnyRedirect().contains("lbpfs.bmstu.ru:8003");
        } catch (ParseException ex1) {
            return false;
        }
    }
}
