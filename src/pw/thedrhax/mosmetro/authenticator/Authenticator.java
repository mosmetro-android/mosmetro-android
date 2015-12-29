package pw.thedrhax.mosmetro.authenticator;

import pw.thedrhax.httpclient.HttpClient;
import pw.thedrhax.util.HTMLFormParser;
import pw.thedrhax.util.Util;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Authenticator {
	private StringBuilder log = new StringBuilder();

    public boolean isConnected() {
        try {
            new HttpClient().navigate("https://google.com");
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
	
	// Returns 0 on success, 1 if already connected or wrong network and 2 if error
    public int connect() {
        String page, fields, link;
        HTMLFormParser parser = new HTMLFormParser();
        HttpClient client = new HttpClient().setIgnoreSSL(true);
        DateFormat dateFormat = DateFormat.getDateTimeInstance();

        log("> " + dateFormat.format(new Date()));

        log(">> Проверка сети");
        try {
            client.navigate("http://1.1.1.1/login.html");
        } catch (Exception ex) {
            log(ex.toString());
            log("<< Ошибка: неправильная сеть (вы не в метро?)");
            return 1;
        }

        log(">> Проверка доступа в интернет");
        if (isConnected()) {
            log("<< Уже подключено");
            return 1;
        }

        log("<< Все проверки пройдены\n>> Подключаюсь...");

        client.setIgnoreSSL(true);
        client.setMaxRetries(3);

        log(">>> Получение перенаправления");
        try {
            page = client.navigate("http://vmet.ro").getContent();
        } catch (UnknownHostException ex) {
            log("<<< Ошибка: DNS сервер не ответил (временная неисправность)");
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
            page = client.navigate(link).getContent();
        } catch (SocketTimeoutException ex) {
            log("<<< Ошибка: сервер не отвечает (временная неисправность)");
            return 2;
        } catch (Exception ex) {
            log(Util.exToStr(ex));
            log("<<< Ошибка: страница авторизации не получена");
            return 2;
        }

        fields = parser.parse(page).toString();
        if (fields == null) {
            log("<<< Ошибка: форма авторизации не найдена");
            return 2;
        }

        // Отправка запроса с данными формы
        log(">>> Отправка формы авторизации");
        try {
            client.navigate(link, fields).getContent();
        } catch (Exception ex) {
            log(Util.exToStr(ex));
            log("<<< Ошибка: сервер не ответил или вернул ошибку");
            return 2;
        }

        log(">> Проверка доступа в интернет");
        if (isConnected()) {
            log("<< Соединение успешно установлено :3");
        } else {
            log("<< Ошибка: доступ в интернет отсутствует");
            return 2;
        }

        log("< " + dateFormat.format(new Date()));
        return 0;
    }

    public void log(String message) {
        log.append(message);
        log.append("\n");
        
        System.out.println(message);
    }
    
    public String getLog() {return log.toString();}
}
