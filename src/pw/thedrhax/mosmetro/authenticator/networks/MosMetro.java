package pw.thedrhax.mosmetro.authenticator.networks;

import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiManager;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.select.Elements;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.Authenticator;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Logger;

import java.net.ProtocolException;
import java.util.Map;

public class MosMetro extends Authenticator {
    public static final String SSID = "MosMetro_Free";
    protected String redirect = null;
    private int version = 2;

    public MosMetro (Context context) {
        super(context);
    }

    @Override
    public String getSSID() {
        return "MosMetro_Free";
    }

    @Override
    public RESULT connect() {
        Map<String,String> fields = null;

        if (stopped) return RESULT.INTERRUPTED;
        progressListener.onProgressUpdate(0);

        logger.log(String.format(context.getString(R.string.auth_connecting), getSSID()));

        logger.log(context.getString(R.string.auth_checking_connection));
        CHECK connected = isConnected();
        if (connected == CHECK.CONNECTED) {
            logger.log(context.getString(R.string.auth_already_connected));
            return RESULT.ALREADY_CONNECTED;
        } else if (connected == CHECK.WRONG_NETWORK) {
            logger.log(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_network)
            ));

            if (settings.getBoolean("pref_wifi_restart", true)) {
                logger.log(context.getString(R.string.auth_restarting_wifi));

                WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                manager.reassociate();
            }

            return RESULT.ERROR;
        }

        logger.log(String.format(context.getString(R.string.auth_version), version));

        if (stopped) return RESULT.INTERRUPTED;
        progressListener.onProgressUpdate(25);

        logger.log(context.getString(R.string.auth_auth_page));
        try {
            client.get(redirect, null, pref_retry_count);
            if (version == 2) {
                Uri redirect_uri = Uri.parse(redirect);
                redirect = redirect_uri.getScheme() + "://" + redirect_uri.getHost();
                client.get(redirect + "/auth", null, pref_retry_count);
            }
            logger.log(Logger.LEVEL.DEBUG, client.getPageContent().outerHtml());
        } catch (Exception ex) {
            logger.log(Logger.LEVEL.DEBUG, ex);
            logger.log(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_auth_page)
            ));
            return RESULT.ERROR;
        }

        if (version == 1) {
            try {
                Elements forms = client.getPageContent().getElementsByTag("form");
                if (forms.size() > 1) {
                    logger.log(String.format(
                            context.getString(R.string.error),
                            context.getString(R.string.auth_error_not_registered)
                    ));
                    return RESULT.NOT_REGISTERED;
                }
                fields = Client.parseForm(forms.first());
            } catch (Exception ex) {
                logger.log(String.format(
                        context.getString(R.string.error),
                        context.getString(R.string.auth_error_auth_form)
                ));
                return RESULT.ERROR;
            }
        }

        if (stopped) return RESULT.INTERRUPTED;
        progressListener.onProgressUpdate(50);

        logger.log(context.getString(R.string.auth_auth_form));
        try {
            switch (version) {
                case 1: client.post(redirect, fields, pref_retry_count); break;
                case 2:
                    String csrf_token = client.parseMetaContent("csrf-token");
                    client.setHeader(Client.HEADER_CSRF, csrf_token);
                    logger.log(Logger.LEVEL.DEBUG, "CSRF Token: " + csrf_token);

                    client.setCookie(redirect, "afVideoPassed", "0");
                    client.post(redirect + "/auth/init?segment=metro", null, pref_retry_count);
                    break;
            }
            logger.log(Logger.LEVEL.DEBUG, client.getPageContent().outerHtml());
        } catch (ProtocolException ignored) { // Too many follow-up requests
        } catch (Exception ex) {
            logger.log(Logger.LEVEL.DEBUG, ex);
            logger.log(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_server)
            ));
            return RESULT.ERROR;
        }

        if (stopped) return RESULT.INTERRUPTED;
        progressListener.onProgressUpdate(75);

        logger.log(context.getString(R.string.auth_checking_connection));
        boolean status = false;
        switch (version) {
            case 1: status = (isConnected() == CHECK.CONNECTED); break;
            case 2:
                try {
                    JSONObject response = (JSONObject)new JSONParser().parse(client.getPage());
                    status = (Boolean)response.get("result");
                } catch (Exception ex) {
                    logger.log(Logger.LEVEL.DEBUG, ex);
                }
                break;
        }
        if (status) {
            logger.log(context.getString(R.string.auth_connected));
        } else {
            logger.log(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_connection)
            ));
            return RESULT.ERROR;
        }

        progressListener.onProgressUpdate(100);

        return RESULT.CONNECTED;
    }
    
    @Override
    public CHECK isConnected() {
        Client client = new OkHttp().followRedirects(false);
        try {
            client.get("http://wi-fi.ru", null, pref_retry_count);
        } catch (Exception ex) {
            // Server not responding => wrong network
            logger.log(Logger.LEVEL.DEBUG, ex);
            return CHECK.WRONG_NETWORK;
        }

        try {
            redirect = client.parseMetaRedirect();
            logger.log(Logger.LEVEL.DEBUG, client.getPageContent().outerHtml());
            logger.log(Logger.LEVEL.DEBUG, redirect);
        } catch (Exception ex) {
            // Redirect not found => connected
            logger.log(Logger.LEVEL.DEBUG, ex);
            return super.isConnected();
        }

        if (redirect.contains("login.wi-fi.ru")) // Fallback to the first version
            version = 1;

        // Redirect found => not connected
        return CHECK.NOT_CONNECTED;
    }
}
