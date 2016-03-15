package pw.thedrhax.mosmetro.authenticator.networks;

import android.content.Context;
import org.jsoup.select.Elements;
import pw.thedrhax.mosmetro.authenticator.Authenticator;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;

import java.util.HashMap;
import java.util.Map;

public class MosGorTrans extends Authenticator {
    public static final String SSID = "MosGorTrans_Free";

    public MosGorTrans (Context context, boolean automatic) {
        super(context, automatic);
    }

    @Override
    public String getSSID() {
        return "MosGorTrans_Free";
    }

    @Override
    protected int connect() {
        String link;
        Map<String,String> fields;

        logger.log_debug("Подключение к сети " + SSID);
        logger.log_debug(">> Проверка доступа в интернет");
        int connected = isConnected();
        if (connected == CHECK_CONNECTED) {
            logger.log_debug("<< Уже подключено");
            return STATUS_ALREADY_CONNECTED;
        } else if (connected == CHECK_WRONG_NETWORK) {
            logger.log_debug("<< Ошибка: Сеть недоступна или не отвечает");
            return STATUS_ERROR;
        }

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
    
    @Override
    public int isConnected() {
        Client client = new OkHttp().followRedirects(false);
        try {
            client.get("http://mosgortrans.ru", null);
            logger.debug(client.getPageContent().outerHtml());
        } catch (Exception ex) {
            // Server not responding => wrong network
            logger.debug(ex);
            return CHECK_WRONG_NETWORK;
        }

        try {
            if (!client.parseLinkRedirect().contains("mosgortrans.netbynet.ru"))
                throw new Exception("Wrong redirect");
        } catch (Exception ex) {
            // Redirect not found => connected
            logger.debug(ex);
            return CHECK_CONNECTED;
        }

        // Redirect found => not connected
        return CHECK_NOT_CONNECTED;
    }
}
