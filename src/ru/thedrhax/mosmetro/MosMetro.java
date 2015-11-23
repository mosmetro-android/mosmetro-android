package ru.thedrhax.mosmetro;

import android.app.Activity;
import android.os.Bundle;

import android.view.View;
import android.widget.TextView;

import java.io.*;

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
		final TextView messages = (TextView)findViewById(R.id.messages);
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
    	HttpRequest request = new HttpRequest("http://curlmyip.org").connect();
    	System.out.println("Your IP: " + request.getContent() + "\n");
    }
}
