package pw.thedrhax.mosmetro.authenticator.networks;

import android.content.Context;
import okhttp3.RequestBody;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import pw.thedrhax.mosmetro.authenticator.Authenticator;

public class MosMetro extends Authenticator {
    public static String SSID = "\"MosMetro_Free\"";

    public MosMetro (Context context, boolean automatic) {
        super(context, automatic);
    }

    @Override
    public String getSSID() {
        return SSID;
    }

    @Override
    public int connect() {
        Document page;
        RequestBody fields;
        String link;

        progressListener.onProgressUpdate(0);

        logger.log_debug("Подключение к сети " + SSID);
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

        progressListener.onProgressUpdate(20);

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

        progressListener.onProgressUpdate(40);

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

        progressListener.onProgressUpdate(60);

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

        progressListener.onProgressUpdate(80);

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

        progressListener.onProgressUpdate(100);

        return STATUS_CONNECTED;
    }
    
    @Override
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
}
