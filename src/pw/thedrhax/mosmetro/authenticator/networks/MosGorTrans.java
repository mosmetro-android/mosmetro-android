package pw.thedrhax.mosmetro.authenticator.networks;

import android.content.Context;

import pw.thedrhax.mosmetro.R;

public class MosGorTrans extends MosMetro {
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

    @Override
    public RESULT connect() {
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

        if (redirect != null) {
            if (redirect.contains("wi-fi.ru")) provider = PROVIDER.NETBYNET;
            if (redirect.contains("enforta")) provider = PROVIDER.ENFORTA;
        }

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
}
