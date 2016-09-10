package pw.thedrhax.mosmetro.authenticator.networks;

import android.content.Context;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.unbescape.javascript.JavaScriptEscape;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.Authenticator;
import pw.thedrhax.mosmetro.httpclient.CachedRetriever;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.clients.JsoupClient;
import pw.thedrhax.util.Util;

import java.util.HashMap;
import java.util.Map;

public class MosGorTrans extends Authenticator {
    public static final String SSID = "MosGorTrans_Free";

    public static final int PROVIDER_NETBYNET = 1;
    public static final int PROVIDER_ENFORTA = 2;

    private int provider = 0;

    public MosGorTrans (Context context, boolean automatic) {
        super(context, automatic);
        client = new JsoupClient();
    }

    @Override
    public String getSSID() {
        return "MosGorTrans_Free";
    }

    private int connect_netbynet() {
        String link;
        Map<String,String> fields;

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(14);

        logger.log_debug(context.getString(R.string.auth_redirect));
        try {
            client.get("http://mosgortrans.ru", null);
            logger.debug(client.getPageContent().outerHtml());
            
            link = client.getReferer() + "&nobot=2";
            logger.debug(link);
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_redirect)
            ));
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(42);

        logger.log_debug(context.getString(R.string.auth_auth_page));
        try {
            client.get(link, null);
            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_auth_page)
            ));
            return STATUS_ERROR;
        }

        try {
            Elements forms = client.getPageContent().getElementsByTag("form");
            fields = Client.parseForm(forms.first());
        } catch (Exception ex) {
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_auth_form)
            ));
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(56);

        logger.log_debug(context.getString(R.string.auth_auth_form));
        try {
            client.post(link, fields);
            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_server)
            ));
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(70);

        logger.log_debug(context.getString(R.string.auth_request));
        try {
            fields = new HashMap<String, String>();
            fields.put("redirect", "http://curlmyip.org");

            client.post("http://192.168.2.1", fields);
            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_server)
            ));
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(84);

        logger.log_debug(context.getString(R.string.auth_checking_connection));
        if (isConnected() == CHECK_CONNECTED) {
            logger.log_debug(context.getString(R.string.auth_connected));
        } else {
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_connection)
            ));
            return STATUS_ERROR;
        }

        progressListener.onProgressUpdate(100);

        return STATUS_CONNECTED;
    }

    private int connect_enforta() {
        String link;
        Map<String,String> fields;

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(12);

        /*
         *  GET mosgortrans.ru
         *  --
         *  Meta redirect: enforta.ru/login?dst=... > link
         */

        logger.log_debug(context.getString(R.string.auth_redirect));
        try {
            client.get("http://mosgortrans.ru", null);
            logger.debug(client.getPageContent().outerHtml());

            link = client.parseMetaRedirect();
            logger.debug(link);
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_redirect)
            ));
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(24);

        /*
         *  GET enforta.ru/login?dst=... < link
         *  Referer: mosgortrans.ru
         *  --
         *  Form: GET hs.enforta.ru/?mac=...&... > fields, link
         */

        logger.log_debug(context.getString(R.string.auth_initial_page));
        try {
            client.get(link, null);
            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_auth_page)
            ));
            return STATUS_ERROR;
        }

        try {
            Element form = client.getPageContent().getElementsByTag("form").first();
            fields = Client.parseForm(form);
            link = form.attr("action");
        } catch (Exception ex) {
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_auth_form)
            ));
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(36);

        /*
         *  GET hs.enforta.ru/?mac=...&... < link, fields
         *  Referer: enforta.ru/login?dst=...
         *  --
         *  GET hs.enforta.ru/users/hotspotConnection?... < 302
         *  Referer: enforta.ru/login?dst=...
         *  --
         *  JavaScript redirect: / > link
         */

        logger.log_debug(context.getString(R.string.auth_redirect));
        try {
            // We need cookies from this page
            client.get(link, fields);
            link = link.split("/?")[0];
            logger.debug(link);
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_redirect)
            ));
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(48);

        /*
         *  GET hs.enforta.ru < link
         *  Referer: hs.enforta.ru/users/hotspotConnection?...
         *  --
         *  GET hs.enforta.ru/users/hotspotSignin < 302
         *  Referer: hs.enforta.ru/users/hotspotConnection?...
         *  --
         *  Form: POST enforta.ru/login > fields, link
         *  Add fields:
         *   * username < doLogin -> username.value
         *   * password < routeros-md5.hexMD5(chap-id + ? + chap-challenge)
         */

        logger.log_debug(context.getString(R.string.auth_auth_page));
        try {
            client.get(link, null);
            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_auth_page)
            ));
            return STATUS_ERROR;
        }

        try {
            Element form = client.getPageContent().getElementsByTag("form").first();
            link = form.attr("action");
            fields = Client.parseForm(form);

            String password = JavaScriptEscape.unescapeJavaScript(
                    client.match("hexMD5\\((.*?)\\);").replaceAll("('|\"| |\\+)", "")
            );
            String script = new CachedRetriever(context).get("http://hs.enforta.ru/js/devices/routeros-md5.js", "");

            fields.put("username", client.match("username\\.value = \"(.*?)\";"));
            fields.put("password", Util.js(script + "; hash = hexMD5(\"" + password + "\")", "hash"));
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_auth_form)
            ));
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(60);

        /*
         *  POST enfrota.ru/login < link, fields
         *  Referer: hs.enforta.ru/users/hotspotSignin
         *  --
         *  Meta redirect: hs.enforta.ru > link
         */

        logger.log(context.getString(R.string.auth_redirect));
        try {
            client.post(link, fields);
            logger.debug(client.getPageContent().outerHtml());

            link = client.parseMetaRedirect();
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_redirect)
            ));
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(72);

        /*
         *  GET hs.enforta.ru < link
         *  Referer: enforta.ru/login
         *  --
         *  GET hs.enforta.ru/logged/landingPageFrame < 302
         *  Referer: enforta.ru/login
         *  --
         *  GET hs.enforta.ru/c4wportal/advmos2 < 302
         *  Referer: enforta.ru/login
         *  --
         *  GET hs.enforta.ru/users/hotspotConnection?... < 302
         *  Referer: enforta.ru/login
         *  --
         *  GET hs.enforta.ru < 302
         *  Referer: enforta.ru/login
         *  --
         *  Finish!
         */

        logger.log_debug(context.getString(R.string.auth_redirect));
        try {
            client.get(link, null);
            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_redirect)
            ));
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(84);

        logger.log_debug(context.getString(R.string.auth_checking_connection));
        if (isConnected() == CHECK_CONNECTED) {
            logger.log_debug(context.getString(R.string.auth_connected));
        } else {
            logger.log_debug(String.format(
                    context.getString(R.string.error),
                    context.getString(R.string.auth_error_connection)
            ));
            return STATUS_ERROR;
        }

        progressListener.onProgressUpdate(100);

        return STATUS_CONNECTED;
    }

    @Override
    protected int connect() {
        logger.log_debug(String.format(context.getString(R.string.auth_connecting), getSSID()));

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(0);

        logger.log_debug(context.getString(R.string.auth_checking_connection));
        int connected = isConnected();
        if (connected == CHECK_CONNECTED) {
            logger.log_debug(context.getString(R.string.auth_already_connected));
            return STATUS_ALREADY_CONNECTED;
        } else if (connected == CHECK_WRONG_NETWORK) {
            logger.log_debug(String.format(context.getString(R.string.error), context.getString(R.string.auth_error_network)));
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;

        switch (provider) {
            case PROVIDER_NETBYNET:
                logger.log_debug(String.format(
                        context.getString(R.string.auth_provider), "NetByNet"
                ));
                return connect_netbynet();
            case PROVIDER_ENFORTA:
                logger.log_debug(String.format(
                        context.getString(R.string.auth_provider), "Enforta"
                ));
                return connect_enforta();
            default:
                logger.log_debug(String.format(
                        context.getString(R.string.error),
                        context.getString(R.string.auth_error_provider)
                ));
                return STATUS_ERROR;
        }
    }

    @Override
    public int isConnected() {
        Client client = new JsoupClient().followRedirects(false);
        try {
            client.get("http://mosgortrans.ru", null);
            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex) {
            // Server not responding => wrong network
            logger.debug(ex);
            return CHECK_WRONG_NETWORK;
        }

        if (provider == 0)
            if (client.getPageContent().outerHtml().contains("mosgortrans.netbynet.ru")) {
                provider = PROVIDER_NETBYNET;
            } else if (client.getPageContent().outerHtml().contains("enforta.ru")) {
                provider = PROVIDER_ENFORTA;
            } else {
                provider = 0;
            }

        try {
            switch (provider) {
                case PROVIDER_NETBYNET:
                    if (!client.parseLinkRedirect().contains("mosgortrans.netbynet.ru"))
                        throw new Exception("Wrong redirect");
                    break;

                case PROVIDER_ENFORTA:
                    client.parseMetaRedirect();
                    break;

                default:
                    throw new Exception("Unknown network");
            }
        } catch (Exception ex) {
            // Redirect not found => connected
            logger.debug(ex);
            return CHECK_CONNECTED;
        }

        // Redirect found => not connected
        return CHECK_NOT_CONNECTED;
    }
}
