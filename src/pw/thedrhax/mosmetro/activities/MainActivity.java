package pw.thedrhax.mosmetro.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.Authenticator;
import pw.thedrhax.mosmetro.authenticator.AuthenticatorStat;

import java.util.Calendar;

public class MainActivity extends Activity {
    // UI Elements
    private TextView text_description;
    private Button button_debug;
    private Menu menu;

    // Connection sequence
    private Authenticator connection;

    // Push received messages to the UI thread
    private final Handler handler = new Handler() {
        public void handleMessage(Message message) {
            String text = message.getData().getString("text");
            if (text == null) return;

            text_description.append(text);
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
		setContentView(R.layout.main);

        connection = new AuthenticatorStat(this, false) {
            // Send log messages to Handler
            public void log (String message) {
                Message msg = handler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putString("text", message + "\n");
                msg.setData(bundle);
                handler.sendMessage(msg);
            }
        };

        text_description = (TextView)findViewById(R.id.text_description);
        button_debug = (Button)findViewById(R.id.button_debug);
	}

    // ActionBar Menu
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        this.menu = menu;

        // This is here because menu is needed to run setDebug
        Bundle extra = getIntent().getExtras();
        if ((extra != null) && (extra.getString("log") != null)) {
            setDebug(true);
            text_description.setText(extra.getString("log"));
        }

        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;

            case R.id.action_share:
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"mosmetro@thedrhax.pw"});
                intent.putExtra(Intent.EXTRA_SUBJECT, "Отчет MosMetro");
                intent.putExtra(Intent.EXTRA_TEXT, text_description.getText().toString());

                startActivity(Intent.createChooser(intent, "Отправить отчет"));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void button_debug (View view) {
        if ((thread == null) || (!thread.isAlive())) {
            setDebug(true);

            thread = new Thread(task);
            thread.start();
        }
    }

    public void onBackPressed() {
        if (button_debug.getText().equals(getString(R.string.button_debug_retry))) {
            setDebug(false);
        } else {
            super.onBackPressed();
        }
    }

    public void setDebug (boolean debug) {
        if (debug) {
            button_debug.setText(getString(R.string.button_debug_retry));
            text_description.setText("");
            menu.setGroupVisible(R.id.menu_debug, true);
        } else {
            button_debug.setText(getString(R.string.button_debug));
            text_description.setText(getString(R.string.text_description));
            menu.setGroupVisible(R.id.menu_debug, false);
        }
    }
}
