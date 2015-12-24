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
import android.widget.TextView;
import pw.thedrhax.mosmetro.authenticator.Authenticator;
import pw.thedrhax.mosmetro.authenticator.AuthenticatorStat;
import pw.thedrhax.mosmetro.R;

import java.util.Calendar;

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
    private Authenticator connection;

    // Run connection sequence in background thread
    private static Thread thread;
    private final Runnable task = new Runnable() {
        public void run () {
            SharedPreferences settings = getSharedPreferences("MosMetro_Lock", 0);
            SharedPreferences.Editor editor = settings.edit();

            if (connection.connect() < 2) {
                Long time = Calendar.getInstance().getTimeInMillis();
                editor.putLong("LastSuccess", time);
                editor.apply();
            }
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

        text_messages = (TextView)findViewById(R.id.text_messages);
        Bundle extra = getIntent().getExtras();
        if (extra == null) {
            if ((thread == null) || (!thread.isAlive())) {
                thread = new Thread(task);
                thread.start();
            }
        } else {
            text_messages.setText(extra.getString("log"));
        }
    }

    // ActionBar Menu
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.debug, menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;

            case R.id.action_share:
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"mosmetro@thedrhax.pw"});
                intent.putExtra(Intent.EXTRA_SUBJECT, "Отчет MosMetro");
                intent.putExtra(Intent.EXTRA_TEXT, text_messages.getText().toString());

                startActivity(Intent.createChooser(intent, "Отправить отчет"));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
