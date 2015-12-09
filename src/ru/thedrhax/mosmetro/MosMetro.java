package ru.thedrhax.mosmetro;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.view.View;
import android.widget.EditText;

import java.io.*;
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
			client.setUserAgent("Mozilla/5.0 (Linux; Android 5.1.1; A0001 Build/LMY48B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.76 Mobile Safari/537.36");
			client.setTimeout(2000);
			
			// Парсер HTML форм
			HTMLFormParser parser = new HTMLFormParser();
			
			String page,fields,link;

			// Проверка сети
			log(">> Checking network");
			if (client
					.navigate("http://1.1.1.1/login.html")
					.getContent() == null
				) {
				log("<< Wrong network");
				return;
			}
			
			// Проверка соединения с интернетом
			log(">> Checking connection");
			if (client
					.navigate("https://google.ru")
					.getContent() != null
				) {
				log("<< Already connected");
				return;
			}
			
			log("<< All checks passed\n>> Connecting...");
			
			client.setIgnoreSSL(true);
			client.setMaxRetries(3);
			
			// Получение страницы с редиректом
			log(">> Getting redirect page");
			page = client
					.navigate("http://vmet.ro")
					.getContent();
			if (page == null) {
				log("<< Error getting redirect page");
				return;
			} else {
				log(page);
			}
			
			// Выделение ссылки на страницу авторизации
			Pattern pLink = Pattern.compile("https?:[^\"]*");
			Matcher mLinkRedirect = pLink.matcher(page);
			
			log(">> Parsing redirect");
			if (mLinkRedirect.find()) {
				link = mLinkRedirect.group(0);
				log(link);
			} else {
				log("<< Failed to parse redirect");
				return;
			}
			
			// Получение страницы авторизации
			log(">> Getting auth page");
			page = client
					.navigate(link)
					.getContent();
			if (page == null) {
				log("<< Failed to get auth page");
				return;
			} else {
				log(page);
			}
			
			// Парсинг формы авторизации
			log(">> Parsing auth form");
			fields = parser
						.parse(page)
						.toString();
			if (fields == null) {
				log("<< Failed to parse auth form");
				return;
			} else {
				log(fields);
			}
			
			// Отправка запроса с данными формы
			log(">> Submitting auth form");
			page = client
					.navigate(link, fields)
					.getContent();
			if (page == null) {
				log("<< Failed to submit auth form");
				return;
			}
			
			// Проверка соединения с интернетом
			log(">> Checking connection");
			if (client
					.navigate("https://google.ru")
					.getContent() != null
				) {
				log("<< Connected successfully! :3");
			} else {
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
	};
	
	private final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			EditText messages = (EditText)findViewById(R.id.messages);
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
