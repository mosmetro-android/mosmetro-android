package pw.thedrhax.mosmetro.authenticator;

import android.content.Context;
import android.net.wifi.WifiManager;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.util.Logger;

public class Chooser {
    private Context context;
    private Logger logger;

    public Chooser(Context context, Logger logger) {
        this.logger = logger;
        this.context = context;
    }

    public Authenticator choose (String SSID) {
        if (SSID == null) return choose();

        logger.log(String.format(context.getString(R.string.chooser_searching), SSID));

        // Trying to match one of Authenticators for this SSID
        Class<? extends Authenticator> result_class = null;
        for (Class<? extends Authenticator> network : Authenticator.SUPPORTED_NETWORKS) {
            try {
                String class_ssid = (String) network.getField("SSID").get(network);
                if ((SSID.equalsIgnoreCase(class_ssid))) {
                    result_class = network; break;
                }
            } catch (Exception ignored) {}
        }

        if (result_class == null) {
            logger.log(String.format(context.getString(R.string.error),
                    context.getString(R.string.chooser_not_supported)
            ));
            return null;
        }

        // Make instance of matched Authenticator
        Authenticator result;
        try {
            result = result_class
                    .getConstructor(Context.class)
                    .newInstance(context);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return result;
    }

    private Authenticator choose() {
        // Get SSID from WifiManager
        logger.log(context.getString(R.string.chooser_ssid));
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String SSID = manager.getConnectionInfo().getSSID().replace("\"", "");
        return choose(SSID);
    }
}
