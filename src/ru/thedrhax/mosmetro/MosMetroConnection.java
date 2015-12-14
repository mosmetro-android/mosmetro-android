package ru.thedrhax.mosmetro;

import ru.thedrhax.httpclient.HttpClient;
import ru.thedrhax.util.Util;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MosMetroConnection {
	//  Returns true on success
    public boolean connect() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        log(">> " + dateFormat.format(new Date()));

        // Блок объявлений
        HttpClient client = new HttpClient();
        client.setTimeout(2000);

        // Парсер HTML форм
        HTMLFormParser parser = new HTMLFormParser();

        String page,fields,link;

        // Проверка сети
        log(">> Checking network");
        try {
            client
                    .navigate("http://1.1.1.1/login.html")
                    .getContent();
        } catch (Exception ex) {
            log("<< Wrong network");
            return true;
        }

        // Проверка соединения с интернетом
        log(">> Checking connection");
        try {
            client
                    .navigate("https://google.ru")
                    .getContent();
            log("<< Already connected");
            return true;
        } catch (Exception ignored) {}

        log("<< All checks passed\n>> Connecting...");

        client.setIgnoreSSL(true);
        client.setMaxRetries(3);

        // Получение страницы с редиректом
        log(">> Getting redirect page");
        try {
            page = client
                    .navigate("http://vmet.ro")
                    .getContent();
        } catch (IOException ex) {
            log("<< Failed to get redirect page: " + Util.exToStr(ex));
            return false;
        } catch (Exception ex) {
            log("<< Unknown exception: " + Util.exToStr(ex));
            return false;
        }

        // Выделение ссылки на страницу авторизации
        Pattern pLink = Pattern.compile("https?:[^\"]*");
        Matcher mLinkRedirect = pLink.matcher(page);

        log(">> Parsing redirect");
        if (mLinkRedirect.find()) {
            link = mLinkRedirect.group(0);
        } else {
            log("<< Redirect link not found");
            return false;
        }

        // Получение страницы авторизации
        log(">> Getting auth page");
        try {
            page = client
                    .navigate(link)
                    .getContent();
        } catch (MalformedURLException ex) {
            log("<< Incorrect redirect URL: " + Util.exToStr(ex));
            return false;
        } catch (SSLHandshakeException ex) {
            log("<< SSL handshake failed: " + Util.exToStr(ex));
            return false;
        } catch (IOException ex) {
            log("<< Failed to get auth page: " + Util.exToStr(ex));
            return false;
        } catch (Exception ex) {
            log("<< Unknown exception: " + Util.exToStr(ex));
            return false;
        }

        // Парсинг формы авторизации
        log(">> Parsing auth form");
        fields = parser
                .parse(page)
                .toString();
        if (fields == null) {
            log("<< Failed to parse auth form");
            return false;
        }

        // Отправка запроса с данными формы
        log(">> Submitting auth form");
        try {
            client
                .navigate(link, fields)
                .getContent();
        } catch (IllegalStateException ex) {
            log("Non critical error: " + Util.exToStr(ex));
        } catch (SSLHandshakeException ex) {
            log("<< SSL handshake failed: " + Util.exToStr(ex));
            return false;
        } catch (IOException ex) {
            log("<< Failed to submit auth form: " + Util.exToStr(ex));
            return false;
        } catch (Exception ex) {
            log("<< Unknown exception: " + Util.exToStr(ex));
            return false;
        }

        client.setIgnoreSSL(false);

        // Проверка соединения с интернетом
        log(">> Checking connection");
        try {
            client
                    .navigate("https://google.ru")
                    .getContent();
            log("<< Connected successfully! :3");
        } catch (Exception ex) {
            log("<< Something wrong happened :C");
            return false;
        }

        log("<< " + dateFormat.format(new Date()));
        return true;
    }

    public void log(String message) {
        System.out.println(message);
    }
}
