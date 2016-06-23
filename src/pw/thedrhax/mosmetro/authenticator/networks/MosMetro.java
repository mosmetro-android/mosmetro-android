package pw.thedrhax.mosmetro.authenticator.networks;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import org.jsoup.select.Elements;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.Authenticator;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;

import java.net.ProtocolException;
import java.util.Map;

public class MosMetro extends Authenticator {
    public static final String SSID = "MosMetro_Free";

    public MosMetro (Context context, boolean automatic) {
        super(context, automatic);
    }

    @Override
    public String getSSID() {
        return "MosMetro_Free";
    }

    @Override
    public int connect() {
        String link;
        Map<String,String> fields;

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(0);

        logger.log_debug(String.format(context.getString(R.string.auth_connecting), getSSID()));

        logger.log_debug(context.getString(R.string.auth_checking_connection));
        int connected = isConnected();
        if (connected == CHECK_CONNECTED) {
            logger.log_debug(context.getString(R.string.auth_already_connected));
            return STATUS_ALREADY_CONNECTED;
        } else if (connected == CHECK_WRONG_NETWORK) {
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_network)
            ));

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            if (settings.getBoolean("pref_wifi_restart", true)) {
                logger.log_debug(context.getString(R.string.auth_restarting_wifi));

                WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                manager.reassociate();
            }

            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(20);

        logger.log_debug(context.getString(R.string.auth_redirect));
        try {
            // TODO: Make the number of retries configurable
            client.get("http://wi-fi.ru", null, 3);
            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_redirect)
            ));
            return STATUS_ERROR;
        }

        try {
            link = client.parseMetaRedirect();
            logger.debug(link);
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_redirect)
            ));
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(40);

        logger.log_debug(context.getString(R.string.auth_auth_page));
        try {
            client.get(link, null, 3);
            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_auth_page)
            ));
            return STATUS_ERROR;
        }

        try {
            Elements forms = client.getPageContent().getElementsByTag("form");
            if (forms.size() > 1) {
                logger.log_debug(String.format(
                        context.getString(R.string.error),
                        context.getString(R.string.auth_error_not_registered)
                ));
                return STATUS_NOT_REGISTERED;
            }
            fields = Client.parseForm(forms.first());
        } catch (Exception ex) {
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_auth_form)
            ));
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(60);

        logger.log_debug(context.getString(R.string.auth_auth_form));
        try {
            client.post(link, fields, 3);
            logger.debug(client.getPageContent().outerHtml());
        } catch (ProtocolException ignored) { // Too many follow-up requests
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_server)
            ));
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(80);

        logger.log_debug(context.getString(R.string.auth_checking_connection));
        if (isConnected() == CHECK_CONNECTED) {
            logger.log_debug(context.getString(R.string.auth_connected));
        } else {
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_connection)
            ));
            return STATUS_ERROR;
        }

        progressListener.onProgressUpdate(100);

        return STATUS_CONNECTED;
    }
    
    @Override
    public int isConnected() {
        Client client;
        try {
            client = new OkHttp()
                    .followRedirects(false)
                    .get("http://wi-fi.ru", null, 3);

            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex) {
            // Server not responding => wrong network
            logger.debug(ex);
            return CHECK_WRONG_NETWORK;
        }

        try {
            client.parseMetaRedirect();
        } catch (Exception ex) {
            // Redirect not found => connected
            logger.debug(ex);
            return CHECK_CONNECTED;
        }

        // Redirect found => not connected
        return CHECK_NOT_CONNECTED;
    }
}
