/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 * Copyright © 2015 Dmitry Karikh <the.dr.hax@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pw.thedrhax.mosmetro.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.services.ConnectionService;
import pw.thedrhax.mosmetro.updater.UpdateCheckTask;

import java.util.List;

public class SettingsActivity extends Activity {
    private SettingsFragment settings;

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_activity, menu);
        return true;
    }

    private void donate_dialog() {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case 0: // Yandex.Money
                        Intent yandex = new Intent(Intent.ACTION_VIEW);
                        yandex.setData(Uri.parse(getString(R.string.donate_yandex_data)));
                        startActivity(yandex);
                        break;

                    case 1: // WebMoney
                        ClipboardManager clipboard = (ClipboardManager)
                                getSystemService(Context.CLIPBOARD_SERVICE);

                        ClipData clip = ClipData.newPlainText("",
                                getString(R.string.donate_webmoney_data)
                        );
                        clipboard.setPrimaryClip(clip);

                        Toast.makeText(SettingsActivity.this,
                                R.string.clipboard_copy,
                                Toast.LENGTH_SHORT
                        ).show();
                        break;

                    case 2: // GitHub
                        Intent github = new Intent(Intent.ACTION_VIEW);
                        github.setData(Uri.parse(getString(R.string.developer_github_repo_link)));
                        startActivity(github);
                        break;

                    case 3: // VK
                        Intent vk = new Intent(Intent.ACTION_VIEW);
                        vk.setData(Uri.parse(getString(R.string.developer_vkontakte_link)));
                        startActivity(vk);
                        break;

                    case 4: // Google Play
                        Intent google = new Intent(Intent.ACTION_VIEW);
                        google.setData(Uri.parse(getString(R.string.developer_google_play_link)));
                        startActivity(google);
                        break;
                }
            }
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.action_donate)
                .setItems(R.array.donate_options, listener)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_donate:
                donate_dialog();
                return true;

            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void update_checker_setup() {
        final ListPreference pref_updater_branch =
                (ListPreference) settings.findPreference("pref_updater_branch");
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

                        if (branch_names.length > 0) {
                            pref_updater_branch.setEntries(branch_names);
                            pref_updater_branch.setEntryValues(branch_names);
                            pref_updater_branch.setEnabled(true);
                        }
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Populate preferences
        settings = new SettingsFragment();
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, settings)
                .commit();
        getFragmentManager().executePendingTransactions();

        // Start/stop service on pref_autoconnect change
        final CheckBoxPreference pref_autoconnect =
                (CheckBoxPreference) settings.findPreference("pref_autoconnect");
        pref_autoconnect.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                Context context = SettingsActivity.this;
                Intent service = new Intent(context, ConnectionService.class);
                if (pref_autoconnect.isChecked())
                    service.setAction("STOP");
                context.startService(service);
                return true;
            }
        });

        update_checker_setup();
    }
}
