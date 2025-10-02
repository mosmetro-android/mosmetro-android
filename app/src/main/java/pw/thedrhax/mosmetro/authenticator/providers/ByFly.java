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

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.FinalConnectionCheckTask;
import pw.thedrhax.mosmetro.authenticator.InitialConnectionCheckTask;
import pw.thedrhax.mosmetro.authenticator.NamedTask;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.httpclient.HtmlForm;
import pw.thedrhax.mosmetro.httpclient.HttpResponse;
import pw.thedrhax.util.Logger;

/**
 * The ByFly class implements support for the public Wi-Fi networks "BELTELECOM"
 * and "byfly WIFI" in Belarus.
 *
 * Detection: Location redirect contains "ciscowifi.beltelecom.by".
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see Provider
 */

public class ByFly extends Provider {
    private static final String CREDS_KEY = "pref_byfly_credentials";
    private String redirect = "https://ciscowifi.beltelecom.by";

    public ByFly(Context context, HttpResponse res) {
        super(context);

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

        add(new NamedTask(context.getString(R.string.auth_auth_page)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                HttpResponse res;

                try {
                    client.setCookie(redirect, "safe", "1");
                    res = client.get(redirect).retry().execute();
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_auth_page)
                    ));
                    return false;
                }

                Elements forms = res.getPageContent().select("form.login-pass-form");

                if (forms.size() == 0) {
                    Logger.log(Logger.LEVEL.DEBUG, res.toString());
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_form)
                    ));
                    return false;
                }

                Logger.log(Logger.LEVEL.DEBUG, forms.first().outerHtml());

                try {
                    HtmlForm form = new HtmlForm(redirect, forms.first());
                    vars.put("form", form);
                } catch (ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_form)
                    ));
                    return false;
                }

                return true;
            }
        });

        add(new NamedTask(context.getString(R.string.auth_auth_form)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                HtmlForm form = (HtmlForm) vars.get("form");

                String login = settings.getString(CREDS_KEY + "_login", "");
                String password = settings.getString(CREDS_KEY + "_password", "");

                if (login.isEmpty() || password.isEmpty()) {
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_byfly_credentials)
                    ));
                    vars.put("result", RESULT.ERROR);
                    return false;
                }

                form.put("login", login);
                form.put("password", password);

                try {
                    HttpResponse res = client.submit(form).execute();
                    Logger.log(Logger.LEVEL.DEBUG, res.toString());
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
            return response.get300Redirect().contains("ciscowifi.beltelecom.by");
        } catch (ParseException ex) {
            return false;
        }
    }
}
