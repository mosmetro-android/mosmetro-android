package pw.thedrhax.mosmetro.authenticator;

import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pw.thedrhax.util.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Authenticator {
    // Result state
    public static final int STATUS_CONNECTED = 0;
    public static final int STATUS_ALREADY_CONNECTED = 1;
    public static final int STATUS_NOT_REGISTERED = 2;
    public static final int STATUS_ERROR = 3;

    // Network check state
    public static final int CHECK_CONNECTED = 0;
    public static final int CHECK_WRONG_NETWORK = 1;
    public static final int CHECK_NOT_CONNECTED = 2;
    
	protected Logger logger;
    private OkHttpClient client;
    private String referer = "http://curlmyip.org";

    public Authenticator () {
        logger = new Logger();
        client = new OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                })
                .cookieJar(new CookieJar() {
                    private HashMap<HttpUrl, List<Cookie>> cookies = new HashMap<HttpUrl, List<Cookie>>();

                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        this.cookies.put(url, cookies);
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        List<Cookie> url_cookies = cookies.get(url);
                        return (url_cookies != null) ? url_cookies : new ArrayList<Cookie>();
                    }
                })
                .build();
    }

    public int isConnected() {
        Document content;
        try {
            content = getPageContent("http://vmet.ro", null);
        } catch (Exception ex) {
            // Server not responding => wrong network
            logger.debug(ex);
            return CHECK_WRONG_NETWORK;
        }

        try {
            parseMetaRedirect(content);
        } catch (Exception ex) {
            // Redirect not found => connected
            logger.debug(ex);
            return CHECK_CONNECTED;
        }

        // Redirect found => not connected
        return CHECK_NOT_CONNECTED;
    }

    private static String parseMetaRedirect (Document document) throws Exception {
        String link = null;

        for (Element element : document.getElementsByTag("meta")) {
            if (element.attr("http-equiv").equalsIgnoreCase("refresh")) {
                String attr = element.attr("content");
                link = attr.substring(attr.indexOf("=") + 1);
            }
        }

        if (link == null || link.isEmpty())
            throw new Exception ("Перенаправление не найдено");

        // Check protocol of the URL
        if (!(link.contains("http://") || link.contains("https://")))
            link = "http://" + link;

        return link;
    }

    private Document getPageContent (String link, RequestBody params) throws Exception {
        Document document;

        // Get and parse the page
        Request.Builder request = new Request.Builder()
                .url(link).addHeader("Referer", referer);

        if (params == null) {
            request = request.get();
        } else {
            request = request.post(params);
        }

        ResponseBody body = client.newCall(request.build())
                .execute().body();
        String content = body.string();
        body.close();

        referer = link;

        if (content == null || content.isEmpty()) {
            throw new Exception("Страница не получена");
        }
        document = Jsoup.parse(content);

        // Clean-up useless tags: <script>, <style>
        document.getElementsByTag("script").remove();
        document.getElementsByTag("style").remove();

        return document;
    }

    private static RequestBody parseForm (Element form) throws Exception {
        Elements inputs = form.getElementsByTag("input");
        FormBody.Builder result = new FormBody.Builder();
        
        for (Element input : inputs)
             result.add(input.attr("name"), input.attr("value"));

        return result.build();
    }

	// Returns 0 on success, 1 if already connected and 2 if error
    public int connect() {
        Document page;
        RequestBody fields;
        String link;

        onChangeProgress(0);

        logger.log_debug(">> Проверка доступа в интернет");
        int connected = isConnected();
        if (connected == CHECK_CONNECTED) {
            logger.log_debug("<< Уже подключено");
            return STATUS_ALREADY_CONNECTED;
        } else if (connected == CHECK_WRONG_NETWORK) {
            logger.log_debug("<< Ошибка: Сеть недоступна или не отвечает");

            logger.log("\nВозможные причины ошибки:");
            logger.log(" * Устройство не полностью подключилось к сети: убедитесь, что статус сети \"Подключено\"");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");

            return STATUS_ERROR;
        }

        logger.log_debug("<< Все проверки пройдены\n>> Подключаюсь...");

        onChangeProgress(20);

        logger.log_debug(">>> Получение начального перенаправления");
        try {
            page = getPageContent("http://vmet.ro", null);
            logger.debug(page.outerHtml());
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: перенаправление не получено");

            logger.log("\nВозможные причины:");
            logger.log(" * Устройство не полностью подключилось к сети: убедитесь, что статус сети \"Подключено\"");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");
            return STATUS_ERROR;
        }

        try {
            link = parseMetaRedirect(page);
            logger.debug(link);
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: перенаправление не найдено");

            logger.log("\nВозможные причины:");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");
            return STATUS_ERROR;
        }

        onChangeProgress(40);

        logger.log_debug(">>> Получение страницы авторизации");
        try {
            page = getPageContent(link, null);
            logger.debug(page.outerHtml());
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: страница авторизации не получена");

            logger.log("\nВозможные причины:");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");
            return STATUS_ERROR;
        }

        try {
            Elements forms = page.getElementsByTag("form");
            if (forms.size() > 1 && forms.last().attr("id").equals("sms-form")) {
                logger.log_debug("<<< Ошибка: устройство не зарегистрировано в сети");

                logger.log("\nВозможные причины:");
                logger.log(" * Ваше устройство подключено к сети в первый раз (вы не проходили регистрацию через СМС)");
                logger.log("   Попробуйте подключиться через браузер и, если потребуется, пройдите регистрацию через СМС.");
                logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");

                return STATUS_NOT_REGISTERED;
            }
            fields = parseForm(forms.first());
        } catch (Exception ex) {
            logger.log_debug("<<< Ошибка: форма авторизации не найдена");

            logger.log("\nВозможные причины:");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");
            return STATUS_ERROR;
        }

        onChangeProgress(60);

        logger.log_debug(">>> Отправка формы авторизации");
        try {
            page = getPageContent(link, fields);
            logger.debug(page.outerHtml());
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: сервер не ответил или вернул ошибку");

            logger.log("\nВозможные причины:");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");
            return STATUS_ERROR;
        }

        onChangeProgress(80);

        logger.log_debug(">> Проверка доступа в интернет");
        if (isConnected() == CHECK_CONNECTED) {
            logger.log_debug("<< Соединение успешно установлено :3");
        } else {
            logger.log_debug("<< Ошибка: доступ в интернет отсутствует");

            logger.log("\nВозможные причины:");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");
            return STATUS_ERROR;
        }

        onChangeProgress(100);

        return STATUS_CONNECTED;
    }

    public void onChangeProgress (int progress) {}

    public void setLogger(Logger logger) {
        this.logger = logger;
    }
    public Logger getLogger() {
        return logger;
    }
}
