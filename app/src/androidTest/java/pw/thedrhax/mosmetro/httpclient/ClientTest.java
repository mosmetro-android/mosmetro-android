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