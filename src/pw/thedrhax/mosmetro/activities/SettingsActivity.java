package pw.thedrhax.mosmetro.activities;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.updater.UpdateCheckTask;

import java.util.List;

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
        final SettingsFragment settings = new SettingsFragment();
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

        /*
            Update checking
         */

        final ListPreference pref_updater_branch = (ListPreference)settings.findPreference("pref_updater_branch");
        pref_updater_branch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                PreferenceManager
                        .getDefaultSharedPreferences(SettingsActivity.this)
                        .edit()
                        .putInt("pref_updater_build", 0)
                        .putInt("pref_updater_ignore", 0)
                        .putString("pref_updater_branch", (String)newValue)
                        .apply();

                new UpdateCheckTask(SettingsActivity.this).execute(false);
                return true;
            }
        });

        // Force check
        Preference pref_updater_check = settings.findPreference("pref_updater_check");
        pref_updater_check.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new UpdateCheckTask(SettingsActivity.this) {
                    @Override
                    public void result(List<Branch> branches) {
                        if (branches == null) return;

                        String[] branch_names = new String[branches.size()];
                        for (int i = 0; i < branch_names.length; i++) {
                            branch_names[i] = branches.get(i).name;
                        }

                        pref_updater_branch.setEntries(branch_names);
                        pref_updater_branch.setEntryValues(branch_names);
                        pref_updater_branch.setEnabled(true);
                    }
                }.setIgnore(preference == null).execute(preference != null);
                return false;
            }
        });

        // Check for updates on start if enabled
        if (PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean("pref_updater_enabled", true))
            pref_updater_check
                    .getOnPreferenceClickListener()
                    .onPreferenceClick(null);
    }
}
