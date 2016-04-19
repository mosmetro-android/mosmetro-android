package pw.thedrhax.mosmetro.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.Authenticator;
import pw.thedrhax.mosmetro.authenticator.Chooser;
import pw.thedrhax.mosmetro.services.ConnectionService;
import pw.thedrhax.util.Logger;

public class DebugActivity extends Activity {
    // UI Elements
    private TextView text_messages;
    
    // Logger
    private Logger logger;
    private boolean show_debug = false;

    // Settings
    private String SSID = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debug_activity);

        text_messages = (TextView)findViewById(R.id.text_messages);

        logger = new Logger() {
            @Override
            public void log(String message) {
                super.log(message);
                if (!show_debug) text_messages.append(message + "\n");
            }

            @Override
            public void debug(String message) {
                super.debug(message);
                if (show_debug) text_messages.append(message + "\n");
            }
        };

        // Check for log from ConnectionService
        try {
            // Intent from the ConnectionService
            Bundle bundle = getIntent().getExtras();
            logger.merge((Logger)bundle.getParcelable("logger"));
            return;
        } catch (NullPointerException ignored) {}

        try {
            // Intent from the SettingsActivity or from shortcuts
            Intent intent = getIntent();
            String SSID = intent.getStringExtra("SSID");

            if (intent.getBooleanExtra("background", false)) {
                Intent service = new Intent(this, ConnectionService.class);
                if (!SSID.isEmpty()) service.putExtra("SSID", SSID);

                startService(service);
                finish();
            }

            if (SSID != null && !SSID.isEmpty())
                this.SSID = "\"" + SSID + "\"";
        } catch (NullPointerException ignored) {}

        button_connect(null);
    }

    // ActionBar Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.debug_activity, menu);

        // Show back button in menu
        try {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException ignored) {}

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                Intent send_email = new Intent(Intent.ACTION_SEND);

                send_email.setType("text/plain");
                send_email.putExtra(Intent.EXTRA_EMAIL, new String[] {getString(R.string.share_email)});
                send_email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
                send_email.putExtra(Intent.EXTRA_TEXT, logger.getDebug());

                startActivity(Intent.createChooser(send_email, getString(R.string.share_client)));
                return true;

            case android.R.id.home:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Run manual connection in background thread
     */

    private class AuthTask extends AsyncTask<String, String, Void> {
        private Logger local_logger;

        public AuthTask() {
            local_logger = new Logger() {
                @Override
                public void log(String message) {
                    super.log(message);
                    publishProgress("log", message);
                }

                @Override
                public void debug(String message) {
                    super.debug(message);
                    publishProgress("debug", message);
                }
            };
        }

        @Override
        protected Void doInBackground(String... params) {
            local_logger.date("> ", "");

            Chooser chooser = new Chooser(DebugActivity.this, false, local_logger);

            Authenticator connection;
            if (params[0] == null) {
                connection = chooser.choose();
            } else {
                connection = chooser.choose(params[0]);
            }
            if (connection == null) return null;

            connection.setLogger(local_logger);
            connection.start();

            local_logger.date("< ", "\n");

            return null;
        }

        // Show log messages in the UI thread
        @Override
        protected void onProgressUpdate(String... values) {
            if (values[0].equals("log")) {
                logger.log(values[1]);
            } else {
                logger.debug(values[1]);
            }
        }
    }

    // Current instance of AuthTask is stored here
    private AuthTask task;

    // Handle manual connection button
    public void button_connect (View view) {
        if ((task == null) || (AsyncTask.Status.FINISHED == task.getStatus()))
            task = new AuthTask();

        if (task.getStatus() != AsyncTask.Status.RUNNING)
            task.execute(SSID);
    }

    // Handle debug log checkbox
    public void show_debug_log (View view) {
        show_debug = ((CheckBox)view).isChecked();
        text_messages.setText(show_debug ? logger.getDebug() : logger.getLog());
    }
}
