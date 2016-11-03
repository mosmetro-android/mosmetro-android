package pw.thedrhax.mosmetro.authenticator.networks;

import android.content.Context;

import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.Authenticator;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Logger;

public class MosGorTrans extends Authenticator {
    public static final String SSID = "MosGorTrans_Free";

    private static enum PROVIDER {
        UNKNOWN, NETBYNET, ENFORTA
    }
    private PROVIDER provider = PROVIDER.UNKNOWN;

    public MosGorTrans (Context context) {
        super(context);
    }

    @Override
    public String getSSID() {
        return "MosGorTrans_Free";
    }

    private RESULT connect_netbynet() {
        String link;
        Map<String,String> fields;

        if (stopped) return RESULT.INTERRUPTED;
        progressListener.onProgressUpdate(14);

        logger.log(context.getString(R.string.auth_redirect));
        try {
            client.get("http://mosgortrans.ru", null);
            logger.log(Logger.LEVEL.DEBUG, client.getPageContent().outerHtml());

            link = client.getReferer() + "&nobot=2";
            logger.log(Logger.LEVEL.DEBUG, link);
        } catch (Exception ex) {
            logger.log(Logger.LEVEL.DEBUG, ex);
            logger.log(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_redirect)
            ));
            return RESULT.ERROR;
        }

        if (stopped) return RESULT.INTERRUPTED;
        progressListener.onProgressUpdate(28);

        logger.log(context.getString(R.string.auth_auth_page));
        try {
            client.get(link, null);
            logger.log(Logger.LEVEL.DEBUG, client.getPageContent().outerHtml());
        } catch (Exception ex) {
            logger.log(Logger.LEVEL.DEBUG, ex);
            logger.log(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_auth_page)
            ));
            return RESULT.ERROR;
        }

        try {
            Elements forms = client.getPageContent().getElementsByTag("form");
            fields = Client.parseForm(forms.first());
        } catch (Exception ex) {
            logger.log(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_auth_form)
            ));
            return RESULT.ERROR;
        }

        if (stopped) return RESULT.INTERRUPTED;
        progressListener.onProgressUpdate(42);

        logger.log(context.getString(R.string.auth_auth_form));
        try {
            client.post(link, fields);
            logger.log(Logger.LEVEL.DEBUG, client.getPageContent().outerHtml());
        } catch (Exception ex) {
            logger.log(Logger.LEVEL.DEBUG, ex);
            logger.log(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_server)
            ));
            return RESULT.ERROR;
        }

        if (stopped) return RESULT.INTERRUPTED;
        progressListener.onProgressUpdate(56);

        logger.log(context.getString(R.string.auth_request));
        try {
            fields = new HashMap<String, String>();
            fields.put("redirect", "http://curlmyip.org");

            client.post("http://192.168.2.1", fields);
            logger.log(Logger.LEVEL.DEBUG, client.getPageContent().outerHtml());
        } catch (Exception ex) {
            logger.log(Logger.LEVEL.DEBUG, ex);
            logger.log(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_server)
            ));
            return RESULT.ERROR;
        }

        if (stopped) return RESULT.INTERRUPTED;
        progressListener.onProgressUpdate(70);

        logger.log(context.getString(R.string.auth_checking_connection));
        if (isConnected() == CHECK.CONNECTED) {
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
    protected RESULT connect() {
        logger.log(String.format(context.getString(R.string.auth_connecting), getSSID()));

        if (stopped) return RESULT.INTERRUPTED;
        progressListener.onProgressUpdate(0);

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
            return RESULT.ERROR;
        }

        if (stopped) return RESULT.INTERRUPTED;

        switch (provider) {
            case NETBYNET:
                logger.log(String.format(
                        context.getString(R.string.auth_provider), "NetByNet"
                ));
                return connect_netbynet();
            case ENFORTA:
                logger.log(String.format(
                        context.getString(R.string.auth_provider), "Enforta"
                ));
                logger.log(String.format(
                        context.getString(R.string.error),
                        String.format(
                                context.getString(R.string.auth_error_provider_unsupported),
                                "Enforta"
                        )
                ));
                return RESULT.UNSUPPORTED;
            default:
                logger.log(String.format(
                        context.getString(R.string.error),
                        context.getString(R.string.auth_error_provider)
                ));
                return RESULT.ERROR;
        }
    }

    @Override
    public CHECK isConnected() {
        Client client = new OkHttp().followRedirects(false);
        try {
            client.get("http://mosgortrans.ru", null);
            logger.log(Logger.LEVEL.DEBUG, client.getPageContent().outerHtml());
        } catch (Exception ex) {
            // Server not responding => wrong network
            logger.log(Logger.LEVEL.DEBUG, ex);
            return CHECK.WRONG_NETWORK;
        }

        if (provider == PROVIDER.UNKNOWN)
            if (client.getPageContent().outerHtml().contains("mosgortrans.netbynet.ru")) {
                provider = PROVIDER.NETBYNET;
            } else if (client.getPageContent().outerHtml().contains("enforta.ru")) {
                provider = PROVIDER.ENFORTA;
            }

        try {
            switch (provider) {
                case NETBYNET:
                    if (!client.parseLinkRedirect().contains("mosgortrans.netbynet.ru"))
                        throw new Exception("Wrong redirect");
                    break;

                case ENFORTA:
                    client.parseMetaRedirect();
                    break;

                default:
                    throw new Exception("Unknown network");
            }
        } catch (Exception ex) {
            // Redirect not found => connected
            logger.log(Logger.LEVEL.DEBUG, ex);
            return CHECK.CONNECTED;
        }

        // Redirect found => not connected
        return CHECK.NOT_CONNECTED;
    }
}
