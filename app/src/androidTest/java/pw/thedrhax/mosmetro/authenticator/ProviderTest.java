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

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import junit.framework.TestCase;

import pw.thedrhax.mosmetro.authenticator.providers.Enforta;
import pw.thedrhax.mosmetro.authenticator.providers.MosMetroV1;
import pw.thedrhax.mosmetro.authenticator.providers.MosMetroV2;
import pw.thedrhax.mosmetro.authenticator.providers.Unknown;
import pw.thedrhax.mosmetro.httpclient.ParsedResponse;

/**
 * A collection of the Provider class tests
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see pw.thedrhax.mosmetro.authenticator.Provider
 */
public class ProviderTest extends TestCase {
    private Context context = InstrumentationRegistry.getContext();

    /**
     * Test automatic Provider detection for predefined server responses.
     * @throws Exception
     */
    public void testFind() throws Exception {
        assertEquals("MosMetro v1 detection",
                Provider.find(context, new ParsedResponse(
                        "<meta http-equiv=\"refresh\" content=\"0; " +
                                "URL=http://login.wi-fi.ru/am/UI/Login?...\" />"
                )).getClass(),

                MosMetroV1.class
        );

        assertEquals("MosMetro v2 detection",
                Provider.find(context, new ParsedResponse(
                        "<meta http-equiv=\"refresh\" content=\"0; " +
                                "URL=http://auth.wi-fi.ru/?rand=...\" />"
                )).getClass(),

                MosMetroV2.class
        );

        assertEquals("Enforta detection",
                Provider.find(context, new ParsedResponse(
                        "<meta http-equiv=\"refresh\" content=\"0; " +
                                "URL=http://...enforta.ru/...\" />"
                )).getClass(),

                Enforta.class
        );

        assertEquals("Empty response",
                Provider.find(context, new ParsedResponse("")).getClass(),

                Unknown.class
        );

        assertEquals("Null response",
                Provider.find(context, (ParsedResponse)null).getClass(),

                Unknown.class
        );
    }

    /**
     * Simple SSID recognition tests
     * @throws Exception
     */
    public void testIsSSIDSupported() throws Exception {
        for (String ssid : Provider.SSIDs) {
            assertTrue(ssid, Provider.isSSIDSupported(ssid));
        }
        assertFalse("<unknown ssid>", Provider.isSSIDSupported("<unknown ssid>"));
        assertFalse("Empty SSID", Provider.isSSIDSupported(""));
    }
}