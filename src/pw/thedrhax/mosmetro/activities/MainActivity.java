package pw.thedrhax.mosmetro.activities;

import android.app.Activity;
import android.content.Intent;
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
import pw.thedrhax.mosmetro.tasks.SendReportTask;

public class MainActivity extends Activity {
    // UI Elements
    private TextView text_description;
    private Button button_debug;
    private Menu menu;
    
    // Connection and logs
    private String connection_debug;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

        text_description = (TextView)findViewById(R.id.text_description);
        button_debug = (Button)findViewById(R.id.button_debug);
	}

    // ActionBar Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        this.menu = menu;

        // This is here because menu is needed to run setDebug()
        try {
            Bundle intent_bundle = getIntent().getExtras();
            String connection_log = intent_bundle.getString("log");
            connection_debug = intent_bundle.getString("debug");
            if (connection_log != null) {
                setDebug(true);
                text_description.setText(connection_log);
            }
        } catch (NullPointerException ignored) {}

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;

            case R.id.action_share:
                new SendReportTask(this, connection_debug).execute();
                return true;

            case android.R.id.home:
                setDebug(false);
                return true;

            default:
                return super.onOptionsItemSelected(item);
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
            if (getActionBar() != null)
                getActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            button_debug.setText(getString(R.string.button_debug));
            text_description.setText(getString(R.string.text_description));
            menu.setGroupVisible(R.id.menu_debug, false);
            if (getActionBar() != null)
                getActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    /*
     * Run manual connection in background thread
     */

    private class AuthTask extends AsyncTask<Void, String, Void> {
        private AuthenticatorStat connection;

        @Override
        protected Void doInBackground(Void... params) {
            connection = new AuthenticatorStat(MainActivity.this, false) {
                // Send log messages as progress
                @Override
                public void log(String message) {
                    super.log(message);
                    publishProgress(message + "\n");
                }
            };
            connection.connect();
            return null;
        }

        // Show log messages in the UI thread
        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            text_description.append(values[0]);
        }

        // Extract debug log after finish
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            connection_debug = connection.getDebug();
        }
    }

    // Current instance of AuthTask is stored here
    private AuthTask task;

    // Handle manual connection button
    public void button_debug (View view) {
        if ((task == null) || (AsyncTask.Status.FINISHED == task.getStatus()))
            task = new AuthTask();

        if (task.getStatus() != AsyncTask.Status.RUNNING) {
            setDebug(true);
            task.execute();
        }
    }
}
