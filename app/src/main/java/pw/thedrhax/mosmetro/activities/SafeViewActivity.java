package pw.thedrhax.mosmetro.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import pw.thedrhax.mosmetro.R;

/**
 * Activity used to call ACTION_VIEW safely
 * (it will not crash if no browsers are installed)
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 */
public class SafeViewActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getIntent().hasExtra("data")) {
            finish(); return;
        }

        Uri data = Uri.parse(getIntent().getStringExtra("data"));
        Intent intent = new Intent(Intent.ACTION_VIEW, data);

        if (getIntent().hasExtra("action")) {
            intent.setAction(getIntent().getStringExtra("action"));
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, R.string.toast_view_exception, Toast.LENGTH_LONG).show();
        }

        finish();
    }
}
