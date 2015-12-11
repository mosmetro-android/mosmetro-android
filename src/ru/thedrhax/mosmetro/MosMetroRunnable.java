package ru.thedrhax.mosmetro;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import ru.thedrhax.httpclient.HttpClient;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MosMetroRunnable implements Runnable {
    private Handler handler;

    public void run () {
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
            return;
        }

        // Проверка соединения с интернетом
        log(">> Checking connection");
        try {
            client
                    .navigate("https://google.ru")
                    .getContent();
            log("<< Already connected");
            return;
        } catch (Exception ex) {}

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
            log("<< Failed to get redirect page: " + exToStr(ex));
            return;
        } catch (Exception ex) {
            log("<< Unknown exception: " + exToStr(ex));
            return;
        }

        // Выделение ссылки на страницу авторизации
        Pattern pLink = Pattern.compile("https?:[^\"]*");
        Matcher mLinkRedirect = pLink.matcher(page);

        log(">> Parsing redirect");
        if (mLinkRedirect.find()) {
            link = mLinkRedirect.group(0);
        } else {
            log("<< Redirect link not found");
            return;
        }

        // Получение страницы авторизации
        log(">> Getting auth page");
        try {
            page = client
                    .navigate(link)
                    .getContent();
        } catch (MalformedURLException ex) {
            log("<< Incorrect redirect URL: " + exToStr(ex));
            return;
        } catch (SSLHandshakeException ex) {
            log("<< SSL handshake failed: " + exToStr(ex));
            return;
        } catch (IOException ex) {
            log("<< Failed to get auth page: " + exToStr(ex));
            return;
        } catch (Exception ex) {
            log("<< Unknown exception: " + exToStr(ex));
            return;
        }

        // Парсинг формы авторизации
        log(">> Parsing auth form");
        fields = parser
                .parse(page)
                .toString();
        if (fields == null) {
            log("<< Failed to parse auth form");
            return;
        }

        // Отправка запроса с данными формы
        log(">> Submitting auth form");
        try {
            page = client
                    .navigate(link, fields)
                    .getContent();
        } catch (IllegalStateException ex) {
            log("Non critical error: " + exToStr(ex));
        } catch (SSLHandshakeException ex) {
            log("<< SSL handshake failed: " + exToStr(ex));
            return;
        } catch (IOException ex) {
            log("<< Failed to submit auth form: " + exToStr(ex));
            return;
        } catch (Exception ex) {
            log("<< Unknown exception: " + exToStr(ex));
            return;
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
        }
    }

    private void log(String text) {
        if (handler == null) return;

        Message msg = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("text", text + "\n");
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    private String exToStr (Exception ex) {
        StringWriter wr = new StringWriter();
        ex.printStackTrace(new PrintWriter(wr));
        return wr.toString();
    }

    public MosMetroRunnable (Handler handler) {
        this.handler = handler;
    }
}
