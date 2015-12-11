package ru.thedrhax.mosmetro;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.view.View;
import android.widget.TextView;

public class MosMetro extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	}

	// Run connection sequence in background thread
	private Thread thread;
	private final Runnable task = new MosMetroRunnable(new Handler() {
		public void handleMessage(Message msg) {
			TextView messages = (TextView)findViewById(R.id.text_messages);
			messages.append(msg.getData().getString("text"));
		}
	});

	// Handle connection button
	public void connect (View view) {
		if ((thread == null) || (!thread.isAlive())) {
			thread = new Thread(task);
			thread.start();
		}
	}
}
