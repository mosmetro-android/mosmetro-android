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

import androidx.annotation.Nullable;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.HttpResponse;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Listener;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Randomizer;

public class Gen204 {
    /**
     * Unreliable generate_204 endpoints (might be intercepted by provider)
     */
    public static final String[] URL_DEFAULT = {
            "connectivitycheck.gstatic.com/generate_204",
            "www.gstatic.com/generate_204",
            "connectivitycheck.android.com/generate_204",
            "play.googleapis.com/generate_204",
            "clients1.google.com/generate_204"
    };

    /**
     * Reliable generate_204 endpoints (confirmed to not be intercepted)
     */
    public static final String[] URL_RELIABLE = {
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

    private Gen204Result last_result = null;

    public Gen204(Context context, Listener<Boolean> running) {
        this.running.subscribe(running);

        client = new OkHttp(context)
                .setFollowRedirects(false)
                .setRunningListener(this.running);

        random = new Randomizer(context);
    }

    /**
     * Perform logged request to specified URL.
     */
    @Nullable
    private HttpResponse request(String schema, String[] urls) {
        for (int i = 0; i < 3; i++) {
            String url = schema + "://" + random.choose(urls);

            try {
                HttpResponse res = client.get(url).execute();
                Logger.log(this, url + " | " + res.getResponseCode());
                return res;
            } catch (IOException ex) {
                Logger.log(this, url + " | " + ex);

                if (ex instanceof SSLPeerUnverifiedException) break;

                if (ex instanceof SSLHandshakeException) {
                    String message = ex.getMessage();

                    if (message == null) break;

                    // Ignore "I/O error during system call, Connection reset by peer"
                    if (message.contains("Connection reset by peer")) continue;

                    break;
                }
            }
        }

        return null;
    }

    public Gen204Result check(boolean expectPositive) {
        HttpResponse rel, unrel;

        if (expectPositive) {
            rel = request("https", URL_RELIABLE);
        } else {
            rel = request("http", URL_RELIABLE);

            if (rel != null && rel.getResponseCode() != 204) {
                return new Gen204Result(rel); // negative
            }
        }

        unrel = request("http", URL_DEFAULT);

        if (rel == null) {
            return new Gen204Result(unrel); // probably negative
        } else {
            Gen204Result res = new Gen204Result(rel, unrel);

            if (res.isFalseNegative() && (last_result == null || !last_result.isFalseNegative())) {
                Logger.log(this, "False negative detected");
            }

            return res; // positive with possible false negative
        }
    }

    public Gen204Result check() {
        HttpResponse unrel, rel_https, rel_http;

        // Unreliable HTTP check (needs to be rechecked by HTTPS)
        unrel = request("http", URL_DEFAULT);

        if (unrel == null) {
            // network is most probably unreachable
            return new Gen204Result();
        }

        // Reliable HTTPS check
        rel_https = request("https", URL_RELIABLE);

        if (unrel.getResponseCode() == 204) {
            if (rel_https == null || rel_https.getResponseCode() != 204) {
                // Reliable HTTP check
                rel_http = request("http", URL_RELIABLE);

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
                if (last_result == null || !last_result.isFalseNegative()) {
                    Logger.log(this, "False negative detected");
                }
                return new Gen204Result(rel_https, unrel);
            }
        }

        Logger.log(this, "Unexpected state");
        return new Gen204Result();
    }

    @Nullable
    public Gen204Result getLastResult() {
        return last_result;
    }

    public class Gen204Result {
        private final HttpResponse response;
        private final HttpResponse falseNegative;

        public Gen204Result(HttpResponse response, HttpResponse falseNegative) {
            if (response == null) {
                this.response = HttpResponse.EMPTY(client);
                this.falseNegative = null;
            } else {
                this.response = response;
                this.falseNegative = falseNegative;
            }

            last_result = this;
        }

        public Gen204Result(HttpResponse response) {
            this(response, null);
        }

        public Gen204Result() {
            this(null);
        }

        public HttpResponse getResponse() {
            return response;
        }

        public boolean isConnected() {
            return response.getResponseCode() == 204;
        }

        public boolean isFalseNegative() {
            return falseNegative != null && falseNegative.getResponseCode() != 204;
        }

        public HttpResponse getFalseNegative() {
            return falseNegative;
        }
    }
}