package pw.thedrhax.mosmetro.authenticator.networks;

import android.content.Context;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Logger;

public class MosGorTrans extends MosMetro {
    public static final String SSID = "MosGorTrans_Free";

    private static enum PROVIDER {
        NETBYNET, ENFORTA
    }
    private PROVIDER provider = PROVIDER.NETBYNET;

    public MosGorTrans (Context context) {
        super(context);
    }

    @Override
    public String getSSID() {
        return "MosGorTrans_Free";
    }

    @Override
    public RESULT connect() {
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
                return super.connect();
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
        // Check for NetByNet
        CHECK result = super.isConnected();
        if (result != CHECK.CONNECTED)
            return result;

        // Check for other networks
        Client client = new OkHttp().followRedirects(false);
        try {
            client.get("http://mosgortrans.ru", null);
            logger.log(Logger.LEVEL.DEBUG, client.getPageContent().outerHtml());
        } catch (Exception ex) {
            // Server not responding => wrong network
            logger.log(Logger.LEVEL.DEBUG, ex);
            return CHECK.WRONG_NETWORK;
        }

        // Provider selection based on previous request
        if (provider == PROVIDER.NETBYNET)
            if (client.getPage().contains("enforta.ru")) {
                provider = PROVIDER.ENFORTA;
            }

        switch (provider) {
            case NETBYNET:
                return CHECK.CONNECTED;

            case ENFORTA:
                try {
                    client.parseMetaRedirect();
                    break;
                } catch (Exception ex) {
                    logger.log(Logger.LEVEL.DEBUG, ex);
                    return CHECK.CONNECTED;
                }
        }

        // Redirect found => not connected
        return CHECK.NOT_CONNECTED;
    }
}
