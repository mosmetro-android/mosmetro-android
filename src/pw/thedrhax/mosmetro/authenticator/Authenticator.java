package pw.thedrhax.mosmetro.authenticator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pw.thedrhax.httpclient.HttpClient;
import pw.thedrhax.util.Logger;

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
    private HttpClient client;

    public Authenticator () {
        logger = new Logger();
        client = new HttpClient().setIgnoreSSL(true);
    }

    public int isConnected() {
        HttpClient client = new HttpClient();

        String content;
        try {
            content = client.navigate("http://vmet.ro").getContent();
            if (content == null || content.isEmpty())
                throw new Exception("Empty response");
        } catch (Exception ex) {
            // Server not responding => wrong network
            return CHECK_WRONG_NETWORK;
        }

        try {
            Document doc = Jsoup.parse(content);
            parseMetaRedirect(doc);
        } catch (Exception ex) {
            // Redirect not found => connected
            return CHECK_CONNECTED;
        }

        // Redirect found => not connected
        return CHECK_NOT_CONNECTED;
    }

    private static String parseMetaRedirect (Document document) throws Exception {
        String link = null;

        for (Element element : document.getElementsByTag("meta")) {
            if (element.attr("http-equiv").equalsIgnoreCase("refresh")) {
                link = element.attr("content").split("URL=")[1];
            }
        }

        if (link == null || link.isEmpty())
            throw new Exception ("Перенаправление не найдено");

        return link;
    }

    private Document getPageContent (String link, String params) throws Exception {
        Document document;

        // Get and parse the page
        String content = client.navigate(link, params).getContent();
        if (content == null || content.isEmpty()) {
            throw new Exception("Страница не получена");
        }
        document = Jsoup.parse(content);

        // Clean-up useless tags: <script>, <style>
        document.getElementsByTag("script").remove();
        document.getElementsByTag("style").remove();

        return document;
    }

    private static String parseForm (Element form) throws Exception {
        String request;
        Elements inputs = form.getElementsByTag("input");

        StringBuilder params = new StringBuilder();
        for (Element input : inputs) {
            if (params.length() != 0) params.append("&");
            params.append(input.attr("name"))
                    .append("=")
                    .append(input.attr("value"));
        }
        request = params.toString();

        if (request == null || request.isEmpty()) {
            throw new Exception("Форма не найдена");
        }

        return request;
    }

	// Returns 0 on success, 1 if already connected and 2 if error
    public int connect() {
        Document page;
        String fields, link;

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

        client.setMaxRetries(3);

        onChangeProgress(16);

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

        onChangeProgress(32);

        logger.log_debug(">>> Получение начальной страницы");
        try {
            page = getPageContent(link, null);
            logger.debug(page.outerHtml());
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: начальная страница не получена");

            logger.log("\nВозможные причины:");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");
            return STATUS_ERROR;
        }

        try {
            fields = parseForm(page.getElementsByTag("form").first());
            logger.debug(fields);
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: форма перенаправления не найдена");

            logger.log("\nВозможные причины:");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");
            return STATUS_ERROR;
        }

        onChangeProgress(48);

        logger.log_debug(">>> Получение страницы авторизации");
        try {
            page = getPageContent(link, fields);
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
            logger.debug(fields);
        } catch (Exception ex) {
            fields = null;
            logger.log_debug("<<< Ошибка: форма авторизации не найдена");

            logger.log("\nВозможные причины:");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");
        }

        onChangeProgress(64);

        logger.log_debug(">>> Отправка формы авторизации");
        try {
            if (fields != null) {
                page = getPageContent(link, fields);
                logger.debug(page.outerHtml());
            }
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
