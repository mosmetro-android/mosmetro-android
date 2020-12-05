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

package pw.thedrhax.mosmetro.httpclient;

import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.ResolverConfig;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import android.content.Context;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import okhttp3.Dns;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.WifiUtils;

public class DnsClient implements Dns {
    private WifiUtils wifi;
    private ExtendedResolver dns;

    private String[] getServers() {
        Set<String> servers = new HashSet<String>();

        wifi.getDns().forEach(new Consumer<InetAddress>() {
            @Override
            public void accept(InetAddress t) {
                servers.add(t.getHostAddress());
            }
        });

        return servers.toArray(new String[servers.size()]);
    }

    private String[] getDefaultServers() {
        String[] config = ResolverConfig.getCurrentConfig().servers();
        return config != null ? config : new String[0];
    }

    public DnsClient(Context context) {
        wifi = new WifiUtils(context);

        String[] servers = getServers();

        if (servers.length == 0) {
            Logger.log(this, "Unable to get servers from Android API");
            servers = getDefaultServers();
        }

        if (servers.length == 0) {
            Logger.log(this, "No servers found, using fallback resolver");
            dns = null;
            return;
        }

        Logger.log(this, String.join(", ", servers));

        try {
            dns = new ExtendedResolver(servers);
        } catch (UnknownHostException ex) {
            Logger.log(Logger.LEVEL.DEBUG, ex);
            Logger.log(this, "Unable to initialize, using fallback resolver");
            dns = null;
        }
    }

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        if (dns == null) {
            return Dns.SYSTEM.lookup(hostname);
        }

        Lookup req;
        try {
            req = new Lookup(hostname, Type.A);
        } catch (TextParseException ex) {
            throw new UnknownHostException(hostname);
        }
        req.setResolver(dns);

        Record[] res = req.run();
        List<InetAddress> result = new LinkedList<>();

        if (res == null || res.length == 0) {
            throw new UnknownHostException(hostname);
        }

        for (Record record : res) {
            if (record instanceof ARecord) {
                result.add(((ARecord) record).getAddress());
            }
        }

        return result;
    }
}
