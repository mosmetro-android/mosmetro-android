package pw.thedrhax.mosmetro.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import pw.thedrhax.mosmetro.R;

public class ShortcutActivity extends Activity {
    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        final Intent sIntent = new Intent(this, DebugActivity.class);
        final Intent.ShortcutIconResource ir = Intent.ShortcutIconResource.
                fromContext(this, R.drawable.ic_launcher);
        final Intent intent = new Intent();

        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, sIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.shortcut_connect));
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, ir);

        setResult(RESULT_OK, intent);
        finish();
    }
}
