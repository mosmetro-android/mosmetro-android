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

package pw.thedrhax.mosmetro.httpclient.clients;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;

import pw.thedrhax.mosmetro.httpclient.Client;

import static org.junit.Assert.*;

/**
 * A collection of the OkHttp class tests
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 */
public class OkHttpTest {
    private Context context = InstrumentationRegistry.getContext();

    @Test
    public void parse302Redirect() throws Exception {
        Client client = new OkHttp(context).followRedirects(false);
        client.get("https://httpstat.us/302", null, 3);
        assertEquals("https://httpstat.us", client.parse302Redirect());
    }

}