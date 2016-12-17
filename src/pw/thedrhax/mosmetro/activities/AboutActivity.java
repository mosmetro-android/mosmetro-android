package pw.thedrhax.mosmetro.activities;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.Menu;
import android.view.MenuItem;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.util.Version;

public class AboutActivity extends Activity {
    public static class AboutFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.about);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Show back button in menu
        try {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException ignored) {}

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Populate preferences
        final AboutFragment settings = new AboutFragment();
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, settings)
                .commit();
        getFragmentManager().executePendingTransactions();

        // Add version name and code
        Preference app_name = settings.findPreference("app_name");
        app_name.setSummary(String.format(
                getString(R.string.version),
                new Version(this).getFormattedVersion())
        );
    }
}
