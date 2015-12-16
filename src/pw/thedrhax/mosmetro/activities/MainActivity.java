package pw.thedrhax.mosmetro.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import pw.thedrhax.mosmetro.R;

import java.util.Calendar;

public class MainActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

        SharedPreferences settings = getSharedPreferences("MosMetro_Lock", 0);
        Long time = Calendar.getInstance().getTimeInMillis();
        Long lastSuccess = settings.getLong("LastSuccess", 0);
        TextView text_last_success = (TextView)findViewById(R.id.text_last_success);
        if (lastSuccess != 0)
            text_last_success.setText(
                    "Успешное подключение: " +
                    Long.toString(((time-lastSuccess)/(1000*60))) +
                    " мин. назад"
            );
	}

    // ActionBar Menu
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void button_debug (View view) {
        startActivity(new Intent(this, DebugActivity.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
