package ru.thedrhax.mosmetro.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import ru.thedrhax.mosmetro.R;

public class MainActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	}

    public void button_debug (View view) {
        Intent debug = new Intent(view.getContext(), DebugActivity.class);
        startActivity(debug);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
