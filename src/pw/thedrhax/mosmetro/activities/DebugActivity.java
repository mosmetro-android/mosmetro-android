package pw.thedrhax.mosmetro.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.Authenticator;
import pw.thedrhax.mosmetro.authenticator.Chooser;
import pw.thedrhax.mosmetro.services.AuthService;
import pw.thedrhax.mosmetro.services.ConnectionService;
import pw.thedrhax.util.Logger;

public class DebugActivity extends Activity {
    public static final String ACTION_DEFAULT = "default";
    public static final String ACTION_SHOW_LOG = "show log";

    public static final String EXTRA_LOGGER = "logger";
    public static final String EXTRA_SSID = "SSID";
    public static final String EXTRA_BACKGROUND = "background";

    // UI Elements
    private TextView text_messages;
    private Button button_connect;
    
    // Logger
    private Logger logger;
    private boolean show_debug = false;

    // Settings
    private String SSID = null;
    private String action = ACTION_DEFAULT;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debug_activity);

        text_messages = (TextView) findViewById(R.id.text_messages);
        button_connect = (Button) findViewById(R.id.button_connect);

        logger = new Logger() {
            @Override
            public void log(LEVEL level, String message) {
                super.log(level, message);

                if ((level == LEVEL.INFO && !show_debug) || (level == LEVEL.DEBUG && show_debug))
                    text_messages.append(message + "\n");
            }
        };

        String intent_action = getIntent().getAction();
        if (intent_action != null) {
            if (ACTION_SHOW_LOG.equals(intent_action)) {
                // Intent from the ConnectionService
                Bundle bundle = getIntent().getExtras();
                logger.merge((Logger) bundle.getParcelable(EXTRA_LOGGER));
            } else {
                // Intent from the SettingsActivity or from shortcuts
                String SSID = getIntent().getStringExtra(EXTRA_SSID);

                if (getIntent().getBooleanExtra(EXTRA_BACKGROUND, false)) {
                    Intent service = new Intent(this, ConnectionService.class);
                    service.putExtras(getIntent());
                    service.setAction(ConnectionService.ACTION_SHORTCUT);
                    startService(service);
                    finish();
                }

                if (SSID != null && !SSID.isEmpty()) this.SSID = SSID;
            }
            action = intent_action;
        }

        bindService(new Intent(this, AuthService.class), auth_conn, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        unbindService(auth_conn);
        super.onDestroy();
    }

    /*
     * ActionBar Menu
     */

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
                send_email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.report_email_subject));
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
     * Buttons and CheckBoxes
     */

    // Handle manual connection button
    public void button_connect (View view) {
        if (button_connect.getText().equals(getString(R.string.cancel))) {
            auth_service.stop();
            return;
        }

        Authenticator connection = new Chooser(this, false, logger).choose(SSID);
        if (connection == null) return;

        auth_service.start(connection);
    }

    // Handle debug log checkbox
    public void show_debug_log (View view) {
        show_debug = ((CheckBox)view).isChecked();
        text_messages.setText("");
        text_messages.append(logger.get(show_debug ? Logger.LEVEL.DEBUG : Logger.LEVEL.INFO));
    }

    /*
     * Binding implementation
     */

    private ServiceConnection auth_conn = new AuthServiceConnection();
    private AuthService.AuthBinder auth_service;

    private class AuthServiceConnection implements ServiceConnection {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            auth_service = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            auth_service = (AuthService.AuthBinder) service;

            auth_service.setLogger(logger);
            auth_service.setCallback(new AuthService.Callback() {
                @Override
                public void onPreExecute() {
                    button_connect.setText(R.string.cancel);
                }

                @Override
                public void onPostExecute(int result) {
                    button_connect.setText(R.string.retry);
                }

                @Override
                public void onCancelled() {
                    logger.log(getString(R.string.interrupted));
                    button_connect.setText(R.string.retry);
                }
            });

            if (!action.equals(ACTION_SHOW_LOG))
                button_connect(null);
        }
    }
}
