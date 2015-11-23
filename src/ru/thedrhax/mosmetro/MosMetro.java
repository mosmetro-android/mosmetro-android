package ru.thedrhax.mosmetro;

import android.app.Activity;
import android.os.Bundle;

import android.view.View;
import android.widget.TextView;

public class MosMetro extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    
    // Handling connection button
    public void connect (View view) {
    	TextView messages = (TextView)findViewById(R.id.messages);
    	
    	messages.append("OK\n");
    }
}
