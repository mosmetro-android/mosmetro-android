/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 * Copyright © 2015 Dmitry Karikh <the.dr.hax@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import pw.thedrhax.util.*;

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
        logger_init();

        // Check for log from ConnectionService
        try {
            // Intent from the ConnectionService
            Bundle bundle = getIntent().getExtras();
            logger.merge((Logger)bundle.getParcelable("logger"));
            if (!getIntent().getBooleanExtra("captcha", false))
                return;
        } catch (NullPointerException ignored) {}

        try {
            // Intent from the SettingsActivity or from shortcuts
            Intent intent = getIntent();
            if (intent.hasExtra("SSID")) {
                SSID = intent.getStringExtra("SSID");
                if (SSID.isEmpty() || WifiUtils.UNKNOWN_SSID.equals(SSID))
                    SSID = new WifiUtils(this).getSSID();
            }
        } catch (NullPointerException ignored) {}

        button_connect(null);
    }

    private void logger_init() {
        text_messages.setText("");

        logger = new Logger() {
            @Override
            public void log(LEVEL level, String message) {
                super.log(level, message);

                if ((level == LEVEL.INFO && !show_debug) || (level == LEVEL.DEBUG && show_debug))
                    text_messages.append(message + "\n");
            }
        };
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

            case R.id.action_clear:
                logger_init();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void button_shortcut (View view) {
        startActivity(new Intent(this, ShortcutActivity.class));
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
