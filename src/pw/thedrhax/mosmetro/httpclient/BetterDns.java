package pw.thedrhax.mosmetro.httpclient;

import android.content.Context;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

/*
 * Get DNS records over HTTPS to avoid DNS blocking
 */

public class BetterDns implements Dns {
    private Context context;

    public BetterDns (Context context) {
        this.context = context;
    }

    @Override
    public List<InetAddress> lookup(String s) throws UnknownHostException {
        List<InetAddress> result = new LinkedList<InetAddress>();

        // Retrieve DNS records from dns-api.org
        String json = new CachedRetriever(context)
                .setClient(new OkHttpClient()) // Override client to avoid stack overflow
                .get("https://dns-api.org/A/" + s, null);

        // Parse JSON answer
        if (json != null) {
            JSONArray records = (JSONArray) JSONValue.parse(json);
            for (Object record : records) {
                try {
                    result.add(InetAddress.getByName(
                            ((JSONObject) record).get("value").toString()
                    ));
                } catch (NullPointerException ignored) {}
            }
        }

        // Fallback to default if failed
        if (result.isEmpty())
            return Dns.SYSTEM.lookup(s);

        return result;
    }
}
