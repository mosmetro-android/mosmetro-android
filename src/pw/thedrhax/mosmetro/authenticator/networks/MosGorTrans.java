package pw.thedrhax.mosmetro.authenticator.networks;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mozilla.javascript.Scriptable;
import pw.thedrhax.mosmetro.authenticator.Authenticator;
import pw.thedrhax.mosmetro.httpclient.CachedRetriever;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.clients.JsoupClient;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        logger.log_debug(">>> Получение начального перенаправления");
        try {
            client.get("http://mosgortrans.ru", null);
            logger.debug(client.getPageContent().outerHtml());
            
            link = client.getReferer() + "&nobot=2";
            logger.debug(link);
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: перенаправление не получено");
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(42);

        logger.log_debug(">>> Получение страницы авторизации");
        try {
            client.get(link, null);
            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: страница авторизации не получена");
            return STATUS_ERROR;
        }

        try {
            Elements forms = client.getPageContent().getElementsByTag("form");
            fields = Client.parseForm(forms.first());
        } catch (Exception ex) {
            logger.log_debug("<<< Ошибка: форма авторизации не найдена");
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(56);

        logger.log_debug(">>> Отправка формы авторизации");
        try {
            client.post(link, fields);
            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: сервер не ответил или вернул ошибку");
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(70);

        logger.log_debug(">>> Отправка последнего запроса");
        try {
            fields = new HashMap<String, String>();
            fields.put("redirect", "http://curlmyip.org");

            client.post("http://192.168.2.1", fields);
            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: сервер не ответил или вернул ошибку");
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(84);

        logger.log_debug(">> Проверка доступа в интернет");
        if (isConnected() == CHECK_CONNECTED) {
            logger.log_debug("<< Соединение успешно установлено :3");
        } else {
            logger.log_debug("<< Ошибка: доступ в интернет отсутствует");
            return STATUS_ERROR;
        }

        progressListener.onProgressUpdate(100);

        return STATUS_CONNECTED;
    }

    private int connect_enforta() {
        String link;
        Map<String,String> fields;

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(7);

        /*
         *  GET mosgortrans.ru
         *  --
         *  Meta redirect: enforta.ru/login?dst=... > link
         */

        logger.log_debug(">>> Получение первого перенаправления");
        try {
            client.get("http://mosgortrans.ru", null);
            logger.debug(client.getPageContent().outerHtml());

            link = client.parseMetaRedirect();
            logger.debug(link);
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: перенаправление не получено");
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(14);

        /*
         *  GET enforta.ru/login?dst=... < link
         *  Referer: mosgortrans.ru
         *  --
         *  Form: GET hs.enforta.ru/?mac=...&... > fields, link
         */

        logger.log_debug(">>> Получение начальной страницы");
        try {
            client.get(link, null);
            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: страница не получена");
            return STATUS_ERROR;
        }

        try {
            Element form = client.getPageContent().getElementsByTag("form").first();
            fields = Client.parseForm(form);
            link = form.attr("action");
        } catch (Exception ex) {
            logger.log_debug("<<< Ошибка: форма не найдена");
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(21);

        /*
         *  GET hs.enforta.ru/?mac=...&... < link, fields
         *  Referer: enforta.ru/login?dst=...
         *  --
         *  300 Redirect: hs.enforta.ru/users/hotspotConnection?... > link
         */

        /*
         *  GET hs.enforta.ru/users/hotspotConnection?... < link
         *  Referer: enforta.ru/login?dst=...
         *  --
         *  JavaScript redirect: / > link
         */

        logger.log_debug(">>> Получение четвертого перенаправления");
        try {
            // We need cookies from this page
            client.get(link, fields);
            link = "http://hs.enforta.ru"; //TODO: Hardcoded URL
            logger.debug(link);
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: перенаправление не получено");
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(28);

        /*
         *  GET hs.enforta.ru < link
         *  Referer: hs.enforta.ru/users/hotspotConnection
         *  --
         *  300 redirect: /webapps/.../splashPage.php?tgr=tr0pn&cbUrl=... > link
         */

        /*
         *  GET hs.enforta.ru/webapps/.../splashPage.php?tgr=tr0pn&cbUrl=... < link
         *  Referer: hs.enforta.ru/users/hotspotConnection?...
         *  --
         *  300 redirect: /?tr0pn=... > link
         */

        /*
         *  GET hs.enforta.ru/?tr0pn=... < link
         *  Referer: hs.enforta.ru/users/hotspotConnection?...
         *  --
         *  Form: POST /users/hotspotSignin > fields, link
         *  Add Fields: data[Signin][username] data[Signin][password]
         */

        logger.log_debug(">>> Получение страницы авторизации");
        try {
            client.get(link, null);
            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: страница авторизации не получена");
            return STATUS_ERROR;
        }

        try {
            Element form = client.getPageContent().getElementsByTag("form").first();
            link = form.attr("action");
            fields = Client.parseForm(form);

            SharedPreferences settings = PreferenceManager
                    .getDefaultSharedPreferences(context);
            fields.put("data[Signin][username]", settings.getString("pref_enforta_username", ""));
            fields.put("data[Signin][password]", settings.getString("pref_enforta_password", ""));
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: форма не найдена");
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(42);

        /*
         *  POST hs.enforta.ru/users/hotspotSignin < link
         *  Referer: hs.enforta.ru/?tr0pn=...
         *  --
         *  300 redirect: red....cloud4wiredirect.com/?rr=... > link
         */

        /*
         *  GET red....cloud4wiredirect.com/?rr=... < link
         *  Referer: hs.enforta.ru/?tr0pn=...
         *  --
         *  Meta redirect: enforta.ru/login?dst=... > link
         */

        logger.log_debug(">>> Получение седьмого перенаправления");
        try {
            client.post(link, fields);
            logger.debug(client.getPageContent().outerHtml());

            link = client.parseMetaRedirect();
            logger.debug(link);
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: перенаправление на найдено");
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(56);

        /*
         *  GET enforta.ru/login?dst=... < link
         *  Referer: red....cloud4wiredirect.com/?rr=...
         *  --
         *  Form: GET hs.enforta.ru > fields, link
         */

        logger.log_debug(">>> Получение восьмого перенаправления");
        try {
            client.get(link, null);
            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: сервер не ответил или вернул ошибку");
            return STATUS_ERROR;
        }

        try {
            Element form = client.getPageContent().getElementsByTag("form").first();
            fields = Client.parseForm(form);
            link = form.attr("action");
        } catch (Exception ex) {
            logger.log_debug("<<< Ошибка: форма не найдена");
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(63);

        /*
         *  GET hs.enforta.ru/?mac=...&... < link, fields
         *  Referer: enforta.ru/login?dst=...
         *  --
         *  300 redirect: /users/hotspotConnection?... > link
         */

        /*
         *  GET hs.enforta.ru/users/hotspotConnection?... < link
         *  Referer: enforta.ru/login?dst=...
         *  --
         *  JavaScript redirect: / > link
         */

        logger.log_debug(">>> Получение десятого перенаправления");
        try {
            // We need cookies from this page
            client.get(link, fields);
            logger.debug(client.getPageContent().outerHtml());

            link = "http://hs.enforta.ru"; // TODO: Hardcoded URL
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: перенаправление не получено");
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(0);

        /*
         *  GET hs.enforta.ru/ < link
         *  Referer: hs.enforta.ru/users/hotspotConnection?...
         *  --
         *  300 redirect: hs.enforta.ru/users/hotspotSignin > link
         */

        /*
         *  GET hs.enforta.ru/users/hotspotSignin < link
         *  Referer: hs.enforta.ru/users/hotspotConnection?...
         *  --
         *  Form: POST enforta.ru/login > fields, link
         *  Add Fields: username password
         */

        logger.log_debug(">>> Получение итоговой страницы авторизации");
        try {
            client.get(link, null);
            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex){
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: страница не получена");
            return STATUS_ERROR;
        }

        try {
            Element form = client.getPageContent().getElementsByTag("form").first();

            // Generate credentials
            Map<String,String> auth = Client.parseForm(form);
            for (Element script : client.getPageContent().getElementsByTag("script")) {
                if (script.outerHtml().contains("function doLogin()")) {
                    Pattern p = Pattern.compile("username\\.value = \"(.*?)\";");
                    Matcher m = p.matcher(script.outerHtml());
                    if (m.find())
                        auth.put("username", m.group(1));
                }
            }

            SharedPreferences settings = PreferenceManager
                    .getDefaultSharedPreferences(context);

            auth.put("password", getPasswordHash(
                    fields.get("chap-id"),
                    settings.getString("pref_enforta_password", ""),
                    fields.get("chap-challenge")
            ));

            link = form.attr("action");
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: форма авторизации не найдена");
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(77);

        /*
         *  POST enforta.ru/login < link, fields
         *  Referer: hs.enforta.ru/users/hotspotSignin
         *  --
         *  Meta redirect: hs.enforta.ru
         */

        logger.log_debug(">>> Отправка последнего запроса");
        try {
            client.post(link, fields);
            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: сервер не ответил или вернул ошибку");
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(84);

        logger.log_debug(">> Проверка доступа в интернет");
        if (isConnected() == CHECK_CONNECTED) {
            logger.log_debug("<< Соединение успешно установлено :3");
        } else {
            logger.log_debug("<< Ошибка: доступ в интернет отсутствует");
            return STATUS_ERROR;
        }

        progressListener.onProgressUpdate(100);

        return STATUS_CONNECTED;
    }

    // TODO: Is Rhino library really necessary?
    private String getPasswordHash (String chap_id, String password, String chap_challenge) throws Exception {
        // Initialize Rhino
        org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
        Scriptable scope = cx.initStandardObjects();

        // Get hashing script
        String script = new CachedRetriever(context).get("http://hs.enforta.ru/js/devices/routeros-md5.js", "");

        // Load hashing script
        cx.evaluateString(scope, script, "", 1, null);

        // Calculate md5(chap-id + password + chap-challenge)
        cx.evaluateString(scope, "hash = hexMD5(\"" + chap_id + password + chap_challenge + "\")", "", 1, null);

        return org.mozilla.javascript.Context.toString(scope.get("hash", scope));
    }

    @Override
    protected int connect() {
        logger.log_debug("Подключение к сети " + SSID);

        if (stopped) return STATUS_INTERRUPTED;
        progressListener.onProgressUpdate(0);

        logger.log_debug(">> Проверка доступа в интернет");
        int connected = isConnected();
        if (connected == CHECK_CONNECTED) {
            logger.log_debug("<< Уже подключено");
            return STATUS_ALREADY_CONNECTED;
        } else if (connected == CHECK_WRONG_NETWORK) {
            logger.log_debug("<< Ошибка: Сеть недоступна или не отвечает");
            return STATUS_ERROR;
        }

        if (stopped) return STATUS_INTERRUPTED;

        switch (provider) {
            case PROVIDER_NETBYNET:
                logger.log_debug(">> Подключение к провайдеру NetByNet");
                return connect_netbynet();
            case PROVIDER_ENFORTA:
                logger.log_debug(">> Подключение к провайдеру Enforta");
                return connect_enforta();
            default:
                logger.log_debug("<< Ошибка: Провайдер не опознан");
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
