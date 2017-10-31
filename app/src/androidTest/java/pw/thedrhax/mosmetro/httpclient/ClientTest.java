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

import junit.framework.TestCase;

import pw.thedrhax.mosmetro.httpclient.clients.DummyClient;

/**
 * A collection of the Client class tests
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see pw.thedrhax.mosmetro.httpclient.Client
 */
public class ClientTest extends TestCase {
    public void testParseMetaRedirect() throws Exception {
        assertEquals("no delay, valid URL",
                "https://example.com/?test=123",
                new DummyClient(
                        "<meta http-equiv=\"refresh\" content=\"https://example.com/?test=123\" />"
                ).parseMetaRedirect()
        );

        assertEquals("no delay, invalid URL: no / before query",
                "https://example.com/?test=123",
                new DummyClient(
                        "<meta name=\"refresh\" content=\"https://example.com?test=123\" />"
                ).parseMetaRedirect()
        );

        assertEquals("no delay, invalid URL: no scheme",
                "http://example.com/?test=123",
                new DummyClient(
                        "<meta name=\"refresh\" content=\"example.com/?test=123\" />"
                ).parseMetaRedirect()
        );

        assertEquals("valid delay, valid URL",
                "https://example.com/?test=123",
                new DummyClient(
                        "<meta name=\"refresh\" content=\"0; url=https://example.com/?test=123\" />"
                ).parseMetaRedirect()
        );

        assertEquals("invalid delay, valid URL",
                "https://example.com/?test=123",
                new DummyClient(
                        "<meta name=\"refresh\" content=\"0;https://example.com/?test=123\" />"
                ).parseMetaRedirect()
        );

        assertEquals("name instead of http-equiv, no delay, valid URL",
                "https://example.com/?test=123",
                new DummyClient(
                        "<meta name=\"refresh\" content=\"https://example.com/?test=123\" />"
                ).parseMetaRedirect()
        );
    }
}