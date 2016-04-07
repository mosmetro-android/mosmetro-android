package pw.thedrhax.mosmetro.authenticator.networks;

import android.content.Context;
import org.jsoup.select.Elements;
import pw.thedrhax.mosmetro.authenticator.Authenticator;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;

import java.net.ProtocolException;
import java.util.Map;

public class MosMetro extends Authenticator {
    public static final String SSID = "\"MosMetro_Free\"";

    public MosMetro (Context context, boolean automatic) {
        super(context, automatic);
    }

    @Override
    public String getSSID() {
        return "\"MosMetro_Free\"";
    }

    @Override
    public int connect() {
        String link;
        Map<String,String> fields;

        progressListener.onProgressUpdate(0);

        logger.log_debug("Подключение к сети " + getSSID());
        logger.log_debug(">> Проверка доступа в интернет");
        int connected = isConnected();
        if (connected == CHECK_CONNECTED) {
            logger.log_debug("<< Уже подключено");
            return STATUS_ALREADY_CONNECTED;
        } else if (connected == CHECK_WRONG_NETWORK) {
            logger.log_debug("<< Ошибка: Сеть недоступна или не отвечает");
            return STATUS_ERROR;
        }

        progressListener.onProgressUpdate(20);

        logger.log_debug(">>> Получение начального перенаправления");
        try {
            client.get("http://vmet.ro", null);
            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: перенаправление не получено");
            return STATUS_ERROR;
        }

        try {
            link = client.parseMetaRedirect();
            logger.debug(link);
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: перенаправление не найдено");
            return STATUS_ERROR;
        }

        progressListener.onProgressUpdate(40);

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
            if (forms.size() > 1 && forms.last().attr("id").equals("sms-form")) {
                logger.log_debug("<<< Ошибка: устройство не зарегистрировано в сети");
                return STATUS_NOT_REGISTERED;
            }
            fields = Client.parseForm(forms.first());
        } catch (Exception ex) {
            logger.log_debug("<<< Ошибка: форма авторизации не найдена");
            return STATUS_ERROR;
        }

        progressListener.onProgressUpdate(60);

        logger.log_debug(">>> Отправка формы авторизации");
        try {
            client.post(link, fields);
            logger.debug(client.getPageContent().outerHtml());
        } catch (ProtocolException ignored) { // Too many follow-up requests
        } catch (Exception ex) {
            logger.debug(ex);
            logger.log_debug("<<< Ошибка: сервер не ответил или вернул ошибку");
            return STATUS_ERROR;
        }

        progressListener.onProgressUpdate(80);

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
    
    @Override
    public int isConnected() {
        Client client;
        try {
            client = new OkHttp()
                    .followRedirects(false)
                    .get("http://vmet.ro", null);

            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex) {
            // Server not responding => wrong network
            logger.debug(ex);
            return CHECK_WRONG_NETWORK;
        }

        try {
            client.parseMetaRedirect();
        } catch (Exception ex) {
            // Redirect not found => connected
            logger.debug(ex);
            return CHECK_CONNECTED;
        }

        // Redirect found => not connected
        return CHECK_NOT_CONNECTED;
    }
}
