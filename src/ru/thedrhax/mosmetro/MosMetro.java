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
			HttpClient client = new HttpClient();
			client.setUserAgent("Mozilla/5.0 (Linux; Android 5.1.1; A0001 Build/LMY48B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.76 Mobile Safari/537.36");
			
			HTMLFormParser parser = new HTMLFormParser();
			
			String page,fields,link;

			// Check network
			if (client.navigate("http://1.1.1.1/login.html")
					.getContent() == null) {
				log("Wrong network");
				return;
			}
			
			// Check internet connection
			if (client.navigate("https://wtfismyip.com/text")
					.getContent() != null) {
				log("Already connected");
				return;
			}
			
			client.setIgnoreSSL(true);
			
			// Get initial redirect
			page = client.navigate("http://vmet.ro").getContent();
			log("== 1. redirect ==\n" + page + "\n=========");

			Pattern pLink = Pattern.compile("https?:[^\"]*");
			Matcher mLinkRedirect = pLink.matcher(page);
			
			if (mLinkRedirect.find()) {
				link = mLinkRedirect.group(0);
				log("Redirect link received: " + link);
			} else {
				log("Redirect failed");
				return;
			}
			
			// Get auth page
			page = client.navigate(link).getContent();
			log("== 2. auth ==\n" + page + "\n=========");
			if (page == null) return;
			
			// Send parsed fields
			fields = parser.parse(page).toString();
			page = client.navigate(link, fields).getContent();
			log("== 3. hidden auth ==\n" + page + "\n=========");
			if (page == null) return;
			
			// Send parsed fields to router
			fields = parser.parse(page).toString();
			page = client.navigate("http://1.1.1.1/login.html", fields).getContent();
			log("== 4. router ==\n" + page + "\n=========");
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
