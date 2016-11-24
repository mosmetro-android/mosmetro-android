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
import pw.thedrhax.util.Version;

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
            public void log(LEVEL level, String message) {
                super.log(level, message);

                if ((level == LEVEL.INFO && !show_debug) || (level == LEVEL.DEBUG && show_debug))
                    text_messages.append(message + "\n");
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
                service.putExtras(intent);
                startService(service);
                finish();
                return;
            }

            if (SSID != null && !SSID.isEmpty()) this.SSID = SSID;
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
                send_email.putExtra(Intent.EXTRA_EMAIL, new String[] {getString(R.string.report_email_address)});
                send_email.putExtra(Intent.EXTRA_SUBJECT, String.format(
                        getString(R.string.report_email_subject), new Version(this).getFormattedVersion()
                ));
                send_email.putExtra(Intent.EXTRA_TEXT, logger.get(Logger.LEVEL.DEBUG));

                startActivity(Intent.createChooser(send_email, getString(R.string.report_choose_client)));
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
                public void log(LEVEL level, String message) {
                    super.log(level, message);
                    publishProgress(level.toString(), message);
                }
            };
        }

        @Override
        protected Void doInBackground(String... params) {
            local_logger.date();

            Chooser chooser = new Chooser(DebugActivity.this, local_logger);

            Authenticator connection = chooser.choose(params[0]);
            if (connection == null) return null;

            connection.setLogger(local_logger);
            connection.start();

            local_logger.date();

            return null;
        }

        // Show log messages in the UI thread
        @Override
        protected void onProgressUpdate(String... values) {
            logger.log(Logger.LEVEL.valueOf(values[0]), values[1]);
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
        text_messages.setText("");
        text_messages.append(logger.get(show_debug ? Logger.LEVEL.DEBUG : Logger.LEVEL.INFO));
    }
}
