package pw.thedrhax.mosmetro.authenticator.providers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;

import org.json.simple.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.NamedTask;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.ParsedResponse;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Logger;

/**
 * The MosMetroV3 class implements support for welcome.wi-fi.ru algorithm.
 *
 * Detection: Meta or Location redirect contains "welcome.wi-fi.ru".
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see Provider
 */

public class MosMetroV3 extends Provider {
    private String redirect = "http://welcome.wi-fi.ru/?client_mac=00-00-00-00-00-00";

    public MosMetroV3(final Context context) {
        super(context);

        /**
         * Checking Internet connection for a first time
         * ⇒ GET generate_204
         * ⇐ Meta + Location redirect: http://welcome.wi-fi.ru/?client_mac=... > redirect, mac
         */
        add(new NamedTask(context.getString(R.string.auth_checking_connection)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                if (isConnected()) {
                    Logger.log(context.getString(R.string.auth_already_connected));
                    vars.put("result", RESULT.ALREADY_CONNECTED);
                    return false;
                } else {
                    if (redirect.contains("client_mac")) {
                        vars.put("mac", Uri.parse(redirect).getQueryParameter("client_mac"));
                    } else {
                        vars.put("mac", "00-00-00-00-00-00");
                    }
                    redirect = ParsedResponse.removePathFromUrl(redirect);
                    return true;
                }
            }
        });

        /**
         * Getting auth page
         * ⇒ GET redirect + /?client_mac=mac < redirect, mac
         * ⇐ 200 OK
         * ⇐ Meta csrf-token > token
         */
        add(new NamedTask(context.getString(R.string.auth_auth_page)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                try {
                    Map<String,String> params = new HashMap<>();
                    params.put("client_mac", (String)vars.get("mac"));

                    client.get(redirect, params, pref_retry_count);
                    Logger.log(Logger.LEVEL.DEBUG, client.response().getPageContent().outerHtml());
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_auth_page)
                    ));
                    return false;
                }

                try {
                    vars.put("token", client.response().parseMetaContent("csrf-token"));
                } catch (ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            "CSRF token not found"
                    ));
                    return false;
                }

                return true;
            }
        });

        /**
         * Initializing auth procedure
         * ⇒ POST redirect + /auth/init < redirect
         * ⇒ JSON: { "authenticity_token": token, "client_mac": mac, "client_ip": "" } < token, mac
         * ⇐ JSON: { "result": true, "user_mac": ..., "auth_status": "initial" }
         */
        add(new NamedTask("Initializing auth procedure") {
            @Override @SuppressLint("HardwareIds")
            public boolean run(HashMap<String, Object> vars) {
                try {
                    JSONObject body = new JSONObject();
                    body.put("authenticity_token", vars.get("token"));
                    body.put("client_mac", vars.get("mac"));
                    body.put("client_ip", "");

                    client.post(
                            redirect + "/auth/init",
                            "application/json; charset=UTF-8",
                            body.toJSONString(),
                            pref_retry_count
                    );
                    Logger.log(Logger.LEVEL.DEBUG, client.response().getPage());
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_server)
                    ));
                    return false;
                }

                try {
                    JSONObject answer = client.response().json();
                    boolean result = answer.containsKey("result") && answer.get("result").equals(true);

                    if (!result) {
                        Logger.log(context.getString(R.string.error,
                                "Unexpected answer: false"
                        ));
                        return false;
                    }
                } catch (org.json.simple.parser.ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_server)
                    ));
                    return false;
                }

                return true;
            }
        });

        /**
         * Checking auth status
         * ⇒ GET redirect + /auth/check?client_mac=mac&client_ip= < redirect, mac
         * ⇐ TODO: 304 Not Modified?
         */
        add(new NamedTask("Checking auth status") {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                try {
                    Map<String,String> params = new HashMap<>();
                    params.put("client_mac", (String)vars.get("mac"));
                    params.put("client_ip", "");

                    client.get(redirect + "/auth/check", params, pref_retry_count);
                    Logger.log(Logger.LEVEL.DEBUG, client.response().getPage());
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                }
                return true;
            }
        });

        /**
         * Finishing auth procedure
         * ⇒ GET redirect + /success?client_mac=mac < redirect, mac
         * ⇐ Location redirect > client.response()
         */
        add(new NamedTask("Finishing auth procedure") {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                try {
                    Map<String,String> params = new HashMap<>();
                    params.put("client_mac", (String)vars.get("mac"));

                    client.get(redirect + "/success", params, pref_retry_count);
                    Logger.log(Logger.LEVEL.DEBUG, client.response().getPage());
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

        /**
         * Checking Internet connection
         * < client.response()
         * ⇐ GOOD: 204 No Content
         * ⇐ BAD: Meta + Location redirect: http://welcome.wi-fi.ru/?client_mac=... > redirect, mac
         * ⇐ OKAY: Meta + Location redirect: http://auth.wi-fi.ru/?segment=... > redirect
         */
        add(new NamedTask(context.getString(R.string.auth_checking_connection)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                Provider provider = Provider.find(context, client.response())
                        .setCallback(callback).setClient(client);

                if (provider instanceof Unknown && isConnected()) {
                    Logger.log(context.getString(R.string.auth_connected));
                    vars.put("result", RESULT.CONNECTED);
                } else if (provider instanceof MosMetroV3) {
                    Logger.log(context.getString(R.string.error,
                            "Infinite loop!"
                    ));
                } else {
                    Logger.log("Switching to another algorithm: " + provider.getName());
                    RESULT result = provider.start();
                    vars.put("result", result);
                }

                return false;
            }
        });
    }

    @Override
    public boolean isConnected() {
        Client client = new OkHttp(context).followRedirects(false);
        try {
            client.get("http://" + random.choose(GENERATE_204), null, pref_retry_count);
        } catch (IOException ex) {
            Logger.log(Logger.LEVEL.DEBUG, ex);
            return false;
        }

        try {
            redirect = client.response().parseMetaRedirect();
            Logger.log(Logger.LEVEL.DEBUG, redirect);
        } catch (ParseException ex) {
            // Redirect not found => connected
            return super.isConnected();
        }

        // Redirect found => not connected
        return false;
    }

    /**
     * Checks if current network is supported by this Provider implementation.
     * @param response  Instance of ParsedResponse.
     * @return          True if response matches this Provider implementation.
     */
    public static boolean match(ParsedResponse response) {
        try {
            return response.parseMetaRedirect().contains("welcome.wi-fi.ru");
        } catch (ParseException ex) {
            return false;
        }
    }
}
