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

import org.json.simple.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.InitialConnectionCheckTask;
import pw.thedrhax.mosmetro.authenticator.NamedTask;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.authenticator.Task;
import pw.thedrhax.mosmetro.httpclient.HttpResponse;
import pw.thedrhax.util.Logger;

/**
 * The AuthLastochkaCenter class implements support for the public Wi-Fi network "Lastochka.Center".
 *
 * Detection: Location redirect contains "auth.lastochka.center"
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see Provider
 */
public class AuthLastochkaCenter extends Provider {
    private String redirect = null;

    public AuthLastochkaCenter(Context context, HttpResponse response) {
        super(context);

        add(new InitialConnectionCheckTask(this, response) {
            @Override
            public boolean handle_response(HashMap<String, Object> vars, HttpResponse response) {
                try {
                    redirect = response.parseAnyRedirect();
                } catch (ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_redirect)
                    ));
                    return false;
                }

                Uri uri = Uri.parse(redirect);
                String url = uri.getQueryParameter("loginurl");
                if (url != null && !url.isEmpty()) {
                    uri = Uri.parse(url);
                }

                vars.put("mac", uri.getQueryParameter("mac"));

                return false;
            }
        });

        add(new NamedTask(context.getString(R.string.auth_auth_page)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                try {
                    HttpResponse res = client.get(redirect).setTries(pref_retry_count).execute();
                    Logger.log(Logger.LEVEL.DEBUG, res.toString());
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_auth_page)
                    ));
                    return false;
                }

                return true;
            }
        });

        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                String url = HttpResponse.removePathFromUrl(redirect);
                url += "/api/checkauthfree/";
                url += vars.get("mac");

                try {
                    HttpResponse res = client.get(url).setTries(pref_retry_count).execute();
                    Logger.log(Logger.LEVEL.DEBUG, res.toString());
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                }

                return true;
            }
        });

        add(new NamedTask(context.getString(R.string.auth_auth_form)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                String url = HttpResponse.removePathFromUrl(redirect);
                url += "/api_local/login?username=";
                url += vars.get("mac");

                try {
                    HttpResponse res = client.get(url).setTries(pref_retry_count).execute();
                    Logger.log(Logger.LEVEL.DEBUG, res.toString());

                    try {
                        JSONObject data = res.json();

                        if (!"True".equals(data.get("status"))) {
                            throw new ParseException("Unexpected result: False", 0);
                        }
                    } catch (org.json.simple.parser.ParseException|NullPointerException ex) {
                        Logger.log(Logger.LEVEL.DEBUG, "Unable to parse: response is not JSON");
                    }
                } catch (IOException|ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_server)
                    ));
                    return false;
                }

                return true;
            }
        });

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

    public static boolean match(HttpResponse response) {
        try {
            return response.parseAnyRedirect().contains("auth.lastochka.center");
        } catch (ParseException ex) {
            return false;
        }
    }
}
