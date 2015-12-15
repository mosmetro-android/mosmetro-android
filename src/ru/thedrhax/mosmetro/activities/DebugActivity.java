package ru.thedrhax.mosmetro.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.TextView;
import ru.thedrhax.mosmetro.MosMetroConnection;
import ru.thedrhax.mosmetro.R;

public class DebugActivity extends Activity {
    // UI Elements
    private TextView text_messages;

    // Push received messages to the UI thread
    private final Handler handler = new Handler() {
        public void handleMessage(Message message) {
            String text = message.getData().getString("text");
            if (text == null) return;

            text_messages.append(text);
        }
    };

    // Connection sequence
    private final MosMetroConnection connection = new MosMetroConnection() {
        // Send log messages to Handler
        public void log (String message) {
            Message msg = handler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putString("text", message + "\n");
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
    };

    // Run connection sequence in background thread
    private static Thread thread;
    private final Runnable task = new Runnable() {
        public void run () {
            connection.connect();
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug);

        try {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException ignored) {}

        text_messages = (TextView)findViewById(R.id.text_messages);

        if ((thread == null) || (!thread.isAlive())) {
            thread = new Thread(task);
            thread.start();
        }
    }

    // Going back to main layout on hardware back button press
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            goBack(); return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    // ... or on menu back button press
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home: goBack(); break;
        }
        return super.onOptionsItemSelected(item);
    }
    private void goBack() {
        Intent main = new Intent(this, MainActivity.class);
        startActivity(main);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
