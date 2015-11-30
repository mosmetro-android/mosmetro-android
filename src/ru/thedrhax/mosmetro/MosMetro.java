package ru.thedrhax.mosmetro;

import android.app.Activity;
import android.os.Bundle;

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
        
        // Redirect stdout to TextView "messages"
		final EditText messages = (EditText)findViewById(R.id.messages);
		System.setOut(new PrintStream(new OutputStream() {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			@Override public void write(int oneByte) throws IOException {
				outputStream.write(oneByte);
				messages.setText(new String(outputStream.toByteArray()));
			}
		}));
	}

	// Handling connection button
	public void connect (View view) {
		HttpClient client = new HttpClient();
		HTMLFormParser parser = new HTMLFormParser();
		
		String page,fields,link;

		// Check network
		if (client.navigate("http://1.1.1.1/login.html").getContent() == null) {
			System.out.println("Wrong network");
			return;
		}
		
		// Check internet connection
		if (client.navigate("https://wtfismyip.com/text").getContent() != null) {
			System.out.println("Already connected");
			return;
		}
		
		client.setIgnoreSSL(true);
		
		// Get initial redirect
		page = client.navigate("http://vmet.ro").getContent();
		System.out.println("== 1. redirect ==\n" + page + "\n=========");

		Pattern pLink = Pattern.compile("https?:[^\"]*");
		Matcher mLinkRedirect = pLink.matcher(page);
		
		if (mLinkRedirect.find()) {
			link = mLinkRedirect.group(0);
			System.out.println("Redirect link received: " + link);
		} else {
			System.out.println("Redirect failed");
			return;
		}
		
		// Get auth page
		page = client.navigate(link).getContent();
		System.out.println("== 2. auth ==\n" + page + "\n=========");
		if (page == null) return;
		
		// Send parsed fields
		fields = parser.parse(page).toString();
		page = client.navigate(link, fields).getContent();
		System.out.println("== 3. hidden auth ==\n" + page + "\n=========");
		if (page == null) return;
		
		// Send parsed fields to router
		fields = parser.parse(page).toString();
		page = client.navigate("http://1.1.1.1/login.html", fields).getContent();
		System.out.println("== 4. router ==\n" + page + "\n=========");
	}
}
