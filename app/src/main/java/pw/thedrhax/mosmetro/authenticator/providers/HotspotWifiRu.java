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

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.InitialConnectionCheckTask;
import pw.thedrhax.mosmetro.authenticator.NamedTask;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.httpclient.HttpRequest;
import pw.thedrhax.mosmetro.httpclient.HttpResponse;
import pw.thedrhax.util.Logger;

public class HotspotWifiRu extends Provider {
    private Element authForm;

    private static Element findForm(Document document) {
        Elements forms = document.body().getElementsByTag("form");

        for (Element form : forms) {
            String action = form.attr("action");

            if (action != null && action.contains("hotspot.wi-fi.ru")) {
                return form;
            }
        }

        return null;
    }

    public HotspotWifiRu(Context context, HttpResponse response) {
        super(context);

        /**
         * Parse login form
         */
        add(new InitialConnectionCheckTask(this, response) {
            @Override
            public boolean handle_response(HashMap<String, Object> vars, HttpResponse response) {
                Element form = findForm(response.getPageContent());

                if (form == null) {
                    Logger.log(context.getString(R.string.error, "Form not found"));
                    return false;
                }

                authForm = form;
                return true;
            }
        });

        /**
         * Send login form to /login
         * Expect meta-redirect to auth.wi-fi.ru/?segment=bus_ttm
         * Switch to another provider
         */
        add(new NamedTask(context.getString(R.string.auth_auth_form)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                HttpRequest request;

                String action = authForm.attr("action");
                Map<String,String> params = HttpResponse.parseForm(authForm);

                if ("post".equalsIgnoreCase(authForm.attr("method"))) {
                    request = client.post(action, params);
                } else {
                    request = client.get(action, params);
                }

                HttpResponse res;
                try {
                    res = request.retry().execute();
                    Logger.log(Logger.LEVEL.DEBUG, res.toString());
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_server)
                    ));
                    return false;
                }

                Provider provider = Provider.find(context, res);

                if (provider instanceof Unknown && isConnected()) {
                    Logger.log(context.getString(R.string.auth_connected));
                    vars.put("result", RESULT.CONNECTED);
                } else if (provider instanceof HotspotWifiRu) {
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_loop)
                    ));
                } else {
                    Logger.log(context.getString(R.string.auth_algorithm_switch, provider.getName()));
                    vars.put("switch", provider.getName());
                    add(indexOf(this) + 1, provider);
                    return true;
                }

                return false;
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
        return findForm(response.getPageContent()) != null;
    }
}
