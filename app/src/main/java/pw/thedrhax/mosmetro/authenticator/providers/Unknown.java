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

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.InitialConnectionCheckTask;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.httpclient.HttpRequest;
import pw.thedrhax.mosmetro.httpclient.HttpResponse;
import pw.thedrhax.util.Logger;

/**
 * The Unknown class is used to tell user that this provider is not
 * recognized or that the Internet connection is already available.
 *
 * Detection: Any response not recognized by all previous Provider classes.
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see Provider
 */

public class Unknown extends Provider {

    public Unknown(final Context context, final HttpResponse response) {
        super(context);

        add(new InitialConnectionCheckTask(this, response) {
            @Override
            public boolean handle_response(HashMap<String, Object> vars, HttpResponse response) {
                boolean recheck = false;

                try {
                    HttpResponse res = response;
                    String redirect = res.parseAnyRedirect(); // throws ParseException

                    Logger.log(Logger.LEVEL.DEBUG, "Attempting to follow all redirects");
                    recheck = true;
                    client.setFollowRedirects(false);

                    for (int i = 0; i < 20; i++) {
                        Provider provider = Provider.find(context, res);

                        if (provider instanceof Unknown) {
                            HttpRequest req = client.get(redirect).retry();
                            Logger.log(Logger.LEVEL.DEBUG, res.getRequest().toString());

                            res = req.execute(); // throws IOException
                            Logger.log(Logger.LEVEL.DEBUG, res.toString());

                            redirect = res.parseAnyRedirect(); // throws ParseException
                        } else {
                            Logger.log(context.getString(R.string.auth_algorithm_switch, provider.getName()));
                            vars.put("switch", provider.getName());

                            client.setFollowRedirects(true);

                            provider.setNested(true)
                                    .setRunningListener(running)
                                    .setClient(client)
                                    .start(vars);

                            Logger.log(context.getString(R.string.auth_waiting));
                            if (!running.sleep(5000)) return false;

                            client.setFollowRedirects(false);

                            if (vars.containsKey("post_auth_redirect")) {
                                redirect = (String) vars.remove("post_auth_redirect");
                            } else {
                                redirect = gen_204.check().getResponse().parseAnyRedirect(); // throws ParseException
                            }
                        }
                    }

                    throw new IOException("Too many redirects");
                } catch (IOException|ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                } finally {
                    client.setFollowRedirects(true);
                }

                if (recheck && isConnected()) {
                    Logger.log(context.getString(R.string.auth_connected));
                    vars.put("result", RESULT.CONNECTED);
                    return true;
                }

                Logger.log(context.getString(R.string.error,
                        context.getString(R.string.auth_error_provider)
                ));
                vars.put("result", RESULT.NOT_SUPPORTED);
                return false;
            }
        });
    }
}
