package pw.thedrhax.mosmetro.authenticator;

import pw.thedrhax.httpclient.HTMLFormParser;
import pw.thedrhax.httpclient.HttpClient;
import pw.thedrhax.util.Logger;

import java.text.DateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Authenticator {
	protected Logger logger;

    public Authenticator () {
        logger = new Logger();
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
	
	// Returns 0 on success, 1 if already connected and 2 if error
    public int connect() {
        String page, fields, link;
        HTMLFormParser parser = new HTMLFormParser();
        HttpClient client = new HttpClient().setIgnoreSSL(true);
        DateFormat dateFormat = DateFormat.getDateTimeInstance();

        logger.log_debug("> " + dateFormat.format(new Date()));

        onChangeProgress(0);

        logger.log_debug(">> Проверка доступа в интернет");
        if (isConnected()) {
            logger.log_debug("<< Уже подключено");
            return 1;
        }

        logger.log_debug("<< Все проверки пройдены\n>> Подключаюсь...");

        client.setIgnoreSSL(true);
        client.setMaxRetries(3);

        onChangeProgress(20);

        logger.log_debug(">>> Получение перенаправления на страницу авторизации");
        try {
            page = client.navigate("http://vmet.ro").getContent();
            if (page.isEmpty()) {
                throw new Exception("Страница перенаправления не получена");
            } else {
                logger.debug(page);
            }
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: перенаправление не получено");

            logger.log("\nВозможные причины:");
            logger.log(" * Устройство не полностью подключилось к сети: убедитесь, что статус сети \"Подключено\"");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");

            logger.log("\nОтчетов по этой ошибке еще не приходило.");
            logger.log("Пожалуйста, отправьте отчет при помощи кнопки \"Поделиться\"");
            return 2;
        }

        Pattern pLink = Pattern.compile("https?:[^\"]*");
        Matcher mLinkRedirect = pLink.matcher(page);

        if (mLinkRedirect.find()) {
            link = mLinkRedirect.group(0).replace("http:", "https:");
            logger.debug(link);
        } else {
            logger.log_debug("<<< Ошибка: перенаправление не найдено");

            logger.log("\nВозможные причины:");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");
            return 2;
        }

        onChangeProgress(40);

        logger.log_debug(">>> Получение страницы авторизации");
        try {
            page = client.navigate(link).getContent();
            if (page.isEmpty()) {
                throw new Exception("Страница авторизации не получена");
            } else {
                logger.debug(page);
            }
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: страница авторизации не получена");

            logger.log("\nВозможные причины:");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");

            logger.log("\nОтчетов по этой ошибке еще не приходило.");
            logger.log("Пожалуйста, отправьте отчет при помощи кнопки \"Поделиться\"");
            return 2;
        }

        fields = parser.parse(page).toString();
        if (fields == null) {
            logger.log_debug("<<< Ошибка: форма авторизации не найдена");

            logger.log("\nВозможные причины:");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");

            logger.log("\nОтчетов по этой ошибке еще не приходило.");
            logger.log("Пожалуйста, отправьте отчет при помощи кнопки \"Поделиться\"");
            return 2;
        }

        onChangeProgress(60);

        // Отправка запроса с данными формы
        logger.log_debug(">>> Отправка формы авторизации");
        try {
            page = client.navigate(link, fields).getContent();
            if (page.isEmpty()) {
                throw new Exception("Ответ сервера не не получен");
            } else {
                logger.debug(page);
            }
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: сервер не ответил или вернул ошибку");

            logger.log("\nВозможные причины:");
            logger.log(" * Сеть временно неисправна или перегружена: попробуйте снова или пересядьте в другой поезд");
            logger.log(" * Структура сети изменилась: потребуется обновление алгоритма");

            logger.log("\nОтчетов по этой ошибке еще не приходило.");
            logger.log("Пожалуйста, отправьте отчет при помощи кнопки \"Поделиться\"");
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

            logger.log("\nОтчетов по этой ошибке еще не приходило.");
            logger.log("Пожалуйста, отправьте отчет при помощи кнопки \"Поделиться\"");
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
