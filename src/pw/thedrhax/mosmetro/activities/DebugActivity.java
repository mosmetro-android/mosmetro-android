package pw.thedrhax.mosmetro.activities;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.AuthenticatorStat;
import pw.thedrhax.mosmetro.authenticator.SendReportTask;
import pw.thedrhax.util.Logger;

public class DebugActivity extends Activity {
    // UI Elements
    private TextView text_messages;
    private Button button_debug;
    
    // Logger
    private Logger logger;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debug_activity);

        text_messages = (TextView)findViewById(R.id.text_messages);
        button_debug = (Button)findViewById(R.id.button_connect);

        logger = new Logger() {
            @Override
            public void log(String message) {
                super.log(message);
                text_messages.append(message + "\n");
            }
        };

        // Check for log from ConnectionService
        try {
            Bundle intent_bundle = getIntent().getExtras();
            if (intent_bundle.getBoolean("ConnectionService", false)) {
                logger.log(intent_bundle.getString("log"));
                logger.debug(intent_bundle.getString("debug"));
                return;
            }
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
                new SendReportTask(this, logger).execute();
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

    private class AuthTask extends AsyncTask<Void, String, Void> {
        private AuthenticatorStat connection;

        @Override
        protected Void doInBackground(Void... params) {
            connection = new AuthenticatorStat(DebugActivity.this, false);
            connection.setLogger(new Logger() {
                @Override
                public void log(String message) {
                    super.log(message);
                    publishProgress(message);
                }
            });
            connection.connect();
            return null;
        }

        // Show log messages in the UI thread
        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            logger.log(values[0]);
        }

        // Extract debug log after finish
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            logger.debug(connection.getLogger().getDebug());
        }
    }

    // Current instance of AuthTask is stored here
    private AuthTask task;

    // Handle manual connection button
    public void button_connect (View view) {
        if ((task == null) || (AsyncTask.Status.FINISHED == task.getStatus()))
            task = new AuthTask();

        if (task.getStatus() != AsyncTask.Status.RUNNING)
            task.execute();
    }
}
