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
        try {
            startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(getIntent().getStringExtra("data"))
            ));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, R.string.toast_view_exception, Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
