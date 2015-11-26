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
		String temp;

		// Check network
		if (client.navigate("http://1.1.1.1/login.html").getContent() == null) {
			System.out.println("[MosMetro] Wrong network");
			return;
		}
		
		// Check internet connection
		if (client.navigate("https://wtfismyip.com/text").getContent() != null) {
			System.out.println("[MosMetro] Already connected");
			return;
		}
		
		// Get initial redirect
		temp = client.navigate("http://vmet.ro").getContent();

		System.out.println(
			"==== 1. redirect page ====\n" +
			temp +
			"\n===="
		);

		final Pattern pLink = Pattern.compile(
			".*(https?:[^\"]*).*",
			Pattern.DOTALL
		);
		Matcher mRedirectLink = pLink.matcher(temp);
		
		if (mRedirectLink.matches()) {
			temp = mRedirectLink.group(1);
			System.out.println("[MosMetro] Redirect link received: " + temp);
		} else {
			System.out.println("[MosMetro] Redirect link receiving failed");
			return;
		}
	}
}
