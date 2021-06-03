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

package pw.thedrhax.mosmetro.authenticator;

import java.io.IOException;

import android.content.Context;

import javax.net.ssl.SSLPeerUnverifiedException;

import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.HttpResponse;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Listener;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Randomizer;
import pw.thedrhax.util.Util;

public class Gen204 {
    /**
     * Unreliable generate_204 endpoints (might be intercepted by provider)
     */
    protected static final String[] URL_DEFAULT = {
            "connectivitycheck.gstatic.com/generate_204",
            "www.gstatic.com/generate_204",
            "connectivitycheck.android.com/generate_204",
            "play.googleapis.com/generate_204",
            "clients1.google.com/generate_204"
    };

    /**
     * Reliable generate_204 endpoints (confirmed to not be intercepted)
     */
    protected static final String[] URL_RELIABLE = {
            "www.google.ru/generate_204",
            "www.google.ru/gen_204",
            "google.com/generate_204",
            "gstatic.com/generate_204",
            "maps.google.com/generate_204",
            "mt0.google.com/generate_204",
            "mt1.google.com/generate_204",
            "mt2.google.com/generate_204",
            "mt3.google.com/generate_204",
            "www.google.com/generate_204"
    };

    private final Listener<Boolean> running = new Listener<Boolean>(true);
    private final Client client;
    private final Randomizer random;
    private final int pref_retry_count;

    private Gen204Result last_result = null;

    public Gen204(Context context, Listener<Boolean> running) {
        this.running.subscribe(running);

        client = new OkHttp(context)
                .customDnsEnabled(true)
                .setFollowRedirects(false)
                .setRunningListener(this.running);

        random = new Randomizer(context);
        pref_retry_count = Util.getIntPreference(context, "pref_retry_count", 3);
    }

    /**
     * Perform logged request to specified URL.
     */
    private HttpResponse request(String schema, String[] urls) throws IOException {
        HttpResponse res = HttpResponse.EMPTY(client);
        IOException last_ex = null;

        for (int i = 0; i < pref_retry_count; i++) {
            String url = schema + "://" + random.choose(urls);

            try {
                res = client.get(url).execute();
                last_ex = null;
                Logger.log(this, url + " | " + res.getResponseCode());
                break;
            } catch (IOException ex) {
                Logger.log(this, url + " | " + ex.toString());
                last_ex = ex;

                if (ex instanceof SSLPeerUnverifiedException) {
                    break;
                }
            }
        }

        if (last_ex != null) {
            throw last_ex;
        }

        return res;
    }

    private Gen204Result tripleCheck() {
        HttpResponse unrel, rel_https, rel_http;

        // Unreliable HTTP check (needs to be rechecked by HTTPS)
        try {
            unrel = request("http", URL_DEFAULT);
        } catch (IOException ex) {
            // network is most probably unreachable
            return new Gen204Result(HttpResponse.EMPTY(client));
        }

        // Reliable HTTPS check
        try {
            rel_https = request("https", URL_RELIABLE);
        } catch (IOException ex) {
            rel_https = null;
        }

        if (unrel.getResponseCode() == 204) {
            if (rel_https == null || rel_https.getResponseCode() != 204) {
                // Reliable HTTP check
                try {
                    rel_http = request("http", URL_RELIABLE);
                } catch (IOException ex) {
                    rel_http = null;
                }

                if (rel_http != null && rel_http.getResponseCode() != 204) {
                    Logger.log(this, "False positive detected");
                    return new Gen204Result(rel_http); // false positive
                }
            } else {
                return new Gen204Result(rel_https); // confirmed positive
            }
        } else {
            if (rel_https == null) {
                return new Gen204Result(unrel); // confirmed negative
            } else if (rel_https.getResponseCode() == 204) {
                Logger.log(this, "False negative detected");
                return new Gen204Result(rel_https, unrel);
            }
        }

        Logger.log(this, "Unexpected state");
        return new Gen204Result(HttpResponse.EMPTY(client));
    }

    public Gen204Result check() {
        Gen204Result res = tripleCheck();
        last_result = res;
        return res;
    }

    public Gen204Result getLastResult() {
        return last_result != null ? last_result : check();
    }

    public class Gen204Result {
        private final HttpResponse response;
        private final HttpResponse falseNegative;

        public Gen204Result(HttpResponse response, HttpResponse falseNegative) {
            this.response = response;
            this.falseNegative = falseNegative;
        }

        public Gen204Result(HttpResponse response) {
            this(response, null);
        }

        public HttpResponse getResponse() {
            return response;
        }

        public boolean isConnected() {
            return response.getResponseCode() == 204;
        }

        public boolean isFalseNegative() {
            return falseNegative != null;
        }

        public HttpResponse getFalseNegative() {
            return falseNegative;
        }
    }
}