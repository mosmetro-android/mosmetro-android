package pw.thedrhax.mosmetro.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.updater.UpdateCheckTask;

public class SettingsActivity extends Activity {

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Populate preferences
        SettingsFragment settings = new SettingsFragment();
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, settings)
                .commit();
        getFragmentManager().executePendingTransactions();

        // Add version name and code
        Preference app_name = settings.findPreference("app_name");
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            app_name.setSummary("Версия: " + pInfo.versionName + "-" + pInfo.versionCode);
        } catch (PackageManager.NameNotFoundException ex) {
            app_name.setSummary("");
        }

        // Update checker
        Preference pref_updater_check = settings.findPreference("pref_updater_check");
        pref_updater_check.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new UpdateCheckTask(SettingsActivity.this) {
                    @Override
                    public void result(boolean hasUpdate, final Branch current_branch) {
                        AlertDialog.Builder dialog = new AlertDialog.Builder(SettingsActivity.this)
                                .setTitle(hasUpdate ?
                                        getString(R.string.updater_available) :
                                        getString(R.string.updater_not_available))
                                .setMessage(hasUpdate ?
                                        current_branch.message :
                                        getString(R.string.updater_not_available_message))
                                .setNegativeButton(hasUpdate ?
                                        getString(R.string.cancel) :
                                        getString(R.string.ok), null);

                        if (hasUpdate)
                            dialog = dialog.setPositiveButton(R.string.install,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Intent.ACTION_VIEW);
                                            intent.setData(Uri.parse(current_branch.getDownloadUrl()));
                                            startActivity(intent);
                                        }
                                    });

                        dialog.show();
                    }
                }.execute();
                return false;
            }
        });
    }
}
