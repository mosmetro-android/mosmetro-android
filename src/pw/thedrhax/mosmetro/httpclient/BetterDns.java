package pw.thedrhax.mosmetro.httpclient;

import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

/*
 * Get DNS records over HTTPS to avoid DNS blocking
 */

public class BetterDns implements Dns {
    @Override
    public List<InetAddress> lookup(String s) throws UnknownHostException {
        List<InetAddress> result = new LinkedList<InetAddress>();

        // Retrieve DNS records from dns-api.org
        String json = null;
        try {
            json = new OkHttpClient().newCall(
                    new Request.Builder().url("https://dns-api.org/A/" + s).get().build()
            ).execute().body().string();
        } catch (IOException ignored) {}

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
