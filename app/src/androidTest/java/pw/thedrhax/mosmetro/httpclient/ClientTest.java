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

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * A collection of the Client class tests
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see pw.thedrhax.mosmetro.httpclient.Client
 */
public class ClientTest {
    @Test
    public void requestToString() throws Exception {
        Map<String,String> params = new HashMap<>();

        params.put("test", "123");
        assertEquals("?test=123", Client.requestToString(params));

        params.put("foo", "bar");
        assertEquals("?test=123&foo=bar", Client.requestToString(params));
    }
}