package ru.thedrhax.mosmetro.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import ru.thedrhax.mosmetro.R;

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

    public void button_debug (View view) {
        Intent debug = new Intent(view.getContext(), DebugActivity.class);
        startActivity(debug);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
