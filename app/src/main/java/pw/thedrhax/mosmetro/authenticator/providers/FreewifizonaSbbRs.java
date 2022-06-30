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

import androidx.annotation.Nullable;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.FinalConnectionCheckTask;
import pw.thedrhax.mosmetro.authenticator.FollowRedirectsTask;
import pw.thedrhax.mosmetro.authenticator.InitialConnectionCheckTask;
import pw.thedrhax.mosmetro.authenticator.NamedTask;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.httpclient.HttpResponse;
import pw.thedrhax.util.Logger;

public class FreewifizonaSbbRs extends Provider {
    private String redirect = "https://freewifizona.sbb.rs/free-wifi-zona/index.html";

    public FreewifizonaSbbRs(Context context, HttpResponse res) {
        super(context);
        client.trustAllCerts();

        add(new InitialConnectionCheckTask(this, res) {
            @Override
            public boolean handle_response(HashMap<String, Object> vars, HttpResponse response) {
                try {
                    redirect = response.parseAnyRedirect();
                    Logger.log(Logger.LEVEL.DEBUG, redirect);
                } catch (ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(Logger.LEVEL.DEBUG, "Redirect not found in response, using default");
                }

                return true;
            }
        });

        add(new FollowRedirectsTask(this) {
            @Nullable @Override
            public String getInitialRedirect(HashMap<String, Object> vars) {
                return redirect;
            }

            @Override
            public void getLastRedirect(String url) {
                redirect = url;
            }
        }.setIgnoreErrors(true));

        add(new NamedTask(context.getString(R.string.auth_auth_page)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                redirect = HttpResponse.removePathFromUrl(redirect);
                HttpResponse res;

                try {
                    res = client.get(redirect + "/sbb_cp_be/app.php/session/").retry().execute();
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_server)
                    ));
                    return false;
                }

                DocumentContext json = res.jsonpath();
                Logger.log(Logger.LEVEL.DEBUG, res.jsonpath().jsonString());

                String age = json.read("$.userAge");

                if (age == null || age.isEmpty()) {
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_not_registered)
                    ));

                    vars.put("result", RESULT.NOT_REGISTERED);
                    return false;
                }

                return true;
            }
        });

        add(new NamedTask(context.getString(R.string.auth_auth_form)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                DocumentContext req = JsonPath.parse("{}");
                req.put("$", "username", "freezona");
                req.put("$", "password", "fr33zon@1234");
                req.put("$", "apMacAddress", "");

                client.headers.setHeader("referer", redirect + "/free-wifi-zona/");

                HttpResponse res;

                try {
                    res = client.post(
                            redirect + "/sbb_cp_be/app.php/account/login",
                            req.jsonString(),
                            "application/json"
                    ).retry().execute();

                    Logger.log(Logger.LEVEL.DEBUG, res.jsonpath().jsonString());
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

    public static boolean match(HttpResponse response) {
        try {
            return response.parseAnyRedirect().contains("freewifizona.sbb.rs");
        } catch (ParseException ex) {
            return false;
        }
    }
}
