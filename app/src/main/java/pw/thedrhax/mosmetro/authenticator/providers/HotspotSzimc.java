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
import pw.thedrhax.mosmetro.httpclient.HttpRequest;
import pw.thedrhax.mosmetro.httpclient.HttpResponse;
import pw.thedrhax.util.Logger;

/**
 * The HotspotSzimc class implements support for the public Wi-Fi network "Lastochka.Center".
 *
 * Detection: Location redirect query contains parameter "loginurl" with "hotspot.szimc" or "auth.szimc"
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see Provider
 */
public class HotspotSzimc extends Provider {
    private String redirect;

    public HotspotSzimc(Context context, HttpResponse response) {
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

                return true;
            }
        });

        /**
         * Follow redirect to /www/loginCoova.html
         * Parse form to /prelogin
         */
        add(new NamedTask(context.getString(R.string.auth_redirect)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                HttpResponse res;

                try {
                    res = client.get(redirect).setTries(pref_retry_count).execute();
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_server)
                    ));
                    return false;
                }

                try {
                    Element form = res.getPageContent()
                            .select("form[name=\"wnamlogin\"]")
                            .first();

                    redirect = HttpResponse.absolutePathToUrl(
                        redirect, form == null ? "/prelogin" : form.attr("prelogin")
                    );
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
         * Get /prelogin
         * Parse redirect to auth.szimc/cp/coovachilli
         */
        add(new NamedTask(context.getString(R.string.auth_auth_page)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                client.setFollowRedirects(false);

                HttpResponse res;

                try {
                    res = client.get(redirect).setTries(pref_retry_count).execute();
                    Logger.log(Logger.LEVEL.DEBUG, res.toString());

                    redirect = res.get300Redirect();
                } catch (IOException|ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_redirect)
                    ));
                    return false;
                } finally {
                    client.setFollowRedirects(true);
                }

                return true;
            }
        });

        /**
         * Follow redirect to auth.szimc/cp/coovachilli
         * Extract form
         */
        add(new NamedTask(context.getString(R.string.auth_redirect)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                HttpResponse res;

                try {
                    res = client.get(redirect).setTries(pref_retry_count).execute();
                    Logger.log(res.toString());
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_server)
                    ));
                    return false;
                }

                if (res.getPageContent().select("form[name=\"sms\"]").size() > 0) {
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_not_registered)
                    ));

                    vars.put("result", RESULT.NOT_REGISTERED);
                    return false;
                }

                Element form = res.getPageContent().select("form[name=\"login\"]").first();

                if (form == null) {
                    Logger.log(Logger.LEVEL.DEBUG, "Form not found");
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_server)
                    ));
                    return false;
                }

                vars.put("form", form);
                return true;
            }
        });

        /**
         * Send auth form to /logon
         * Expect redirect to auth.szimc/cp/coovachilli?res=success&...
         */
        add(new NamedTask(context.getString(R.string.auth_auth_form)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                Element form = (Element) vars.get("form");
                HttpRequest req;

                String method = form.attr("method");
                if ("post".equalsIgnoreCase(method)) {
                    req = client.post(form.attr("action"), HttpResponse.parseForm(form));
                } else {
                    req = client.get(form.attr("action"), HttpResponse.parseForm(form));
                }

                client.setFollowRedirects(false);

                HttpResponse res;

                try {
                    res = req.setTries(pref_retry_count).execute();
                    Logger.log(Logger.LEVEL.DEBUG, res.toString());
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_redirect)
                    ));
                    return false;
                } finally {
                    client.setFollowRedirects(true);
                }

                Uri url;

                try {
                    url = Uri.parse(res.get300Redirect());

                    String result = url.getQueryParameter("res");

                    if (!"success".equalsIgnoreCase(result)) {
                        Logger.log(Logger.LEVEL.DEBUG, "Unexpected result: " + result);
                        Logger.log(context.getString(R.string.error,
                                context.getString(R.string.auth_error_server)
                        ));
                        return false;
                    }
                } catch (ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_redirect)
                    ));
                    return false;
                }

                try {
                    res = client.get(url.toString()).setTries(pref_retry_count).execute();
                    Logger.log(Logger.LEVEL.DEBUG, res.toString());
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
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
            Uri redirect = Uri.parse(response.parseAnyRedirect());
            String loginurl = redirect.getQueryParameter("loginurl");
            return loginurl != null && loginurl.matches("^https?://auth\\.szimc.*");
        } catch (ParseException ex) {
            return false;
        }
    }
}
