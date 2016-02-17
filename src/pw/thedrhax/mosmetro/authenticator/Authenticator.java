package pw.thedrhax.mosmetro.authenticator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pw.thedrhax.httpclient.HttpClient;
import pw.thedrhax.util.Logger;

import java.text.DateFormat;
import java.util.Date;

public class Authenticator {
	protected Logger logger;
    private HttpClient client;

    public Authenticator () {
        logger = new Logger();
        client = new HttpClient().setIgnoreSSL(true);
    }

    public boolean isConnected() {
        try {
            new HttpClient().navigate("https://google.com");
            return true;
        } catch (Exception ex) {
            logger.debug(ex);
            return false;
        }
    }

    private static String parseMetaRedirect (String content) throws Exception {
        String link = null;

        for (Element element : Jsoup.parse(content).getElementsByTag("meta")) {
            if (element.attr("http-equiv").equalsIgnoreCase("refresh")) {
                link = element.attr("content").split("URL=")[1];
            }
        }

        if (link == null || link.isEmpty())
            throw new Exception ("Перенаправление не найдено");

        return link;
    }

    private String getPageContent (String link) throws Exception {
        String content = client.navigate(link).getContent();

        if (content == null || content.isEmpty()) {
            throw new Exception("Страница не получена");
        }

        return content;
    }

    private String getPageContent (String link, String params) throws Exception {
        String content = client.navigate(link, params).getContent();

        if (content == null || content.isEmpty()) {
            throw new Exception("Страница не получена");
        }

        return content;
    }

    private static String parseForm (String content) throws Exception {
        String request;

        Elements inputs = Jsoup.parse(content)
                .getElementsByTag("form").first()
                .getElementsByTag("input");

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
        String page, fields, link;
        DateFormat dateFormat = DateFormat.getDateTimeInstance();

        logger.log_debug("> " + dateFormat.format(new Date()));

        onChangeProgress(0);

        logger.log_debug(">> Проверка доступа в интернет");
        if (isConnected()) {
            logger.log_debug("<< Уже подключено");
            return 1;
        }

        logger.log_debug("<< Все проверки пройдены\n>> Подключаюсь...");

        client.setMaxRetries(3);

        onChangeProgress(16);

        logger.log_debug(">>> Получение начального перенаправления");
        try {
            page = getPageContent("http://vmet.ro");
            logger.debug(page);
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: перенаправление не получено");

            logger.log("\nВозможные причины:");
            logger.log(" * Устройство не полностью подключилось к сети: убедитесь, что статус сети \"Подключено\"");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");
            return 2;
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
            return 2;
        }

        onChangeProgress(32);

        logger.log_debug(">>> Получение начальной страницы");
        try {
            page = getPageContent(link);
            logger.debug(page);
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: начальная страница не получена");

            logger.log("\nВозможные причины:");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");
            return 2;
        }

        try {
            fields = parseForm(page);
            logger.debug(fields);
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: форма перенаправления не найдена");

            logger.log("\nВозможные причины:");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");
            return 2;
        }

        onChangeProgress(48);

        logger.log_debug(">>> Получение страницы авторизации");
        try {
            page = getPageContent(link, fields);
            logger.debug(page);
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: страница авторизации не получена");

            logger.log("\nВозможные причины:");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");
            return 2;
        }

        try {
            fields = parseForm(page);
            logger.debug(fields);
        } catch (Exception ex) {
            logger.log_debug("<<< Ошибка: форма авторизации не найдена");

            logger.log("\nВозможные причины:");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");
            return 2;
        }

        onChangeProgress(64);

        logger.log_debug(">>> Отправка формы авторизации");
        try {
            page = getPageContent(link, fields);
            logger.debug(page);
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: сервер не ответил или вернул ошибку");

            logger.log("\nВозможные причины:");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");
            return 2;
        }

        onChangeProgress(80);

        logger.log_debug(">> Проверка доступа в интернет");
        if (isConnected()) {
            logger.log_debug("<< Соединение успешно установлено :3");
        } else {
            logger.log_debug("<< Ошибка: доступ в интернет отсутствует");

            logger.log("\nВозможные причины:");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");
            return 2;
        }

        onChangeProgress(100);

        logger.log_debug("< " + dateFormat.format(new Date()));
        return 0;
    }

    public void onChangeProgress (int progress) {}

    public void setLogger(Logger logger) {
        this.logger = logger;
    }
    public Logger getLogger() {
        return logger;
    }
}
