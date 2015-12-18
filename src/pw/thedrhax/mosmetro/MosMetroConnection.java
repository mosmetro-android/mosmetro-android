package pw.thedrhax.mosmetro;

import pw.thedrhax.httpclient.HttpClient;
import pw.thedrhax.util.HTMLFormParser;
import pw.thedrhax.util.Util;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MosMetroConnection {
	//  Returns 0 on success, 1 if already connected and 2 if error
    public int connect() {
        String page, fields, link;
        HTMLFormParser parser = new HTMLFormParser();
        HttpClient client = new HttpClient();
        client.setTimeout(2000);
        DateFormat dateFormat = DateFormat.getDateTimeInstance();

        log("> " + dateFormat.format(new Date()));
        log(">> Проверка сети");
        try {
            client
                    .navigate("http://1.1.1.1/login.html")
                    .getContent();
        } catch (Exception ex) {
            log(Util.exToStr(ex));
            log("<< Ошибка: неправильная сеть");
            return 2;
        }

        log(">> Проверка доступа в интернет");
        try {
            client
                    .navigate("https://google.ru")
                    .getContent();
            log("<< Уже подключено");
            return 1;
        } catch (Exception ignored) {
        }

        log("<< Все проверки пройдены\n>> Подключаюсь...");

        client.setIgnoreSSL(true);
        client.setMaxRetries(3);


        log(">>> Получение перенаправления");
        try {
            page = client
                    .navigate("http://vmet.ro")
                    .getContent();
        } catch (UnknownHostException ex) {
            log("<<< Ошибка: DNS сервер не ответил");
            log("<<< Похоже на временную неисправность");
            return 2;
        } catch (Exception ex) {
            log(Util.exToStr(ex));
            log("<<< Ошибка: перенаправление не получено");
            return 2;
        }

        Pattern pLink = Pattern.compile("https?:[^\"]*");
        Matcher mLinkRedirect = pLink.matcher(page);

        if (mLinkRedirect.find()) {
            link = mLinkRedirect.group(0);
        } else {
            log("<<< Ошибка: перенаправление не найдено");
            return 2;
        }

        log(">>> Получение страницы авторизации");
        try {
            page = client
                    .navigate(link)
                    .getContent();
        } catch (SocketTimeoutException ex) {
            log("<<< Ошибка: сервер не отвечает");
            log("<<< Похоже на временную неисправность");
            return 2;
        } catch (Exception ex) {
            log(Util.exToStr(ex));
            log("<<< Ошибка: страница авторизации не получена");
            return 2;
        }

        fields = parser
                .parse(page)
                .toString();
        if (fields == null) {
            log("<<< Ошибка: форма авторизации не найдена");
            return 2;
        }

        // Отправка запроса с данными формы
        log(">>> Отправка формы авторизации");
        try {
            client
                .navigate(link, fields)
                .getContent();
        } catch (Exception ex) {
            log(Util.exToStr(ex));
            log("<<< Ошибка: сервер не ответил или вернул ошибку");
            return 2;
        }

        client.setIgnoreSSL(false);

        log(">> Проверка доступа в интернет");
        try {
            client
                    .navigate("https://google.ru")
                    .getContent();
            log("<< Соединение успешно установлено :3");
        } catch (Exception ex) {
            log("<< Ошибка: доступ в интернет отсутствует");
            return 2;
        }

        log("< " + dateFormat.format(new Date()));
        return 0;
    }

    public void log(String message) {
        System.out.println(message);
    }
}
