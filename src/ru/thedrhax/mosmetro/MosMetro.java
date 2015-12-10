package ru.thedrhax.mosmetro;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.view.View;
import android.widget.TextView;

import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import java.util.regex.*;

import ru.thedrhax.httpclient.*;

public class MosMetro extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
	}

	private Thread thread;
	private Runnable runnable = new Runnable () {
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
	};
	
	private final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			TextView messages = (TextView)findViewById(R.id.text_messages);
			messages.append(msg.getData().getString("text"));
		}
	};

	// Handling connection button
	public void connect (View view) {
		if ((thread == null) || (!thread.isAlive())) {
			thread = new Thread(runnable);
			thread.start();
		}
	}
}
