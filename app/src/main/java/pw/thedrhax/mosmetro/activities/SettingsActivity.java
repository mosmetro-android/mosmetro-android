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
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.List;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.captcha.CaptchaRecognitionProxy;
import pw.thedrhax.mosmetro.services.ConnectionService;
import pw.thedrhax.mosmetro.updater.UpdateCheckTask;
import pw.thedrhax.util.Downloader;
import pw.thedrhax.util.PermissionUtils;
import pw.thedrhax.util.Version;

public class SettingsActivity extends Activity {
    private SettingsFragment fragment;
    private SharedPreferences settings;

    protected Downloader downloader;

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
                        startActivity(new Intent(SettingsActivity.this, SafeViewActivity.class)
                                .putExtra("data", getString(R.string.donate_yandex_data))
                        );
                        break;

                    case 1: // Bitcoin
                        ClipboardManager clipboard = (ClipboardManager)
                                getSystemService(Context.CLIPBOARD_SERVICE);

                        ClipData clip = ClipData.newPlainText("",
                                getString(R.string.donate_bitcoin_data)
                        );
                        clipboard.setPrimaryClip(clip);

                        Toast.makeText(SettingsActivity.this,
                                R.string.clipboard_copy,
                                Toast.LENGTH_SHORT
                        ).show();
                        break;

                    case 2: // GitHub
                        startActivity(new Intent(SettingsActivity.this, SafeViewActivity.class)
                                .putExtra("data", getString(R.string.developer_github_repo_link))
                        );
                        break;

                    case 3: // VK
                        startActivity(new Intent(SettingsActivity.this, SafeViewActivity.class)
                                .putExtra("data", getString(R.string.developer_vkontakte_link))
                        );
                        break;

                    case 4: // Google Play
                        startActivity(new Intent(SettingsActivity.this, SafeViewActivity.class)
                                .putExtra("data", getString(R.string.developer_google_play_link))
                        );
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
        }
        return super.onOptionsItemSelected(item);
    }

    private void update_checker_setup() {
        final ListPreference pref_updater_branch =
                (ListPreference) fragment.findPreference("pref_updater_branch");
        pref_updater_branch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                settings.edit()
                        .putInt("pref_updater_build", 0)
                        .putInt("pref_updater_ignore", 0)
                        .putString("pref_updater_branch", (String)newValue)
                        .apply();

                new UpdateCheckTask(SettingsActivity.this, downloader).execute(false);
                return true;
            }
        });

        // Force check
        Preference pref_updater_check = fragment.findPreference("pref_updater_check");
        pref_updater_check.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new UpdateCheckTask(SettingsActivity.this, downloader) {
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
        if (settings.getBoolean("pref_updater_enabled", true))
            pref_updater_check
                    .getOnPreferenceClickListener()
                    .onPreferenceClick(null);

        // Extension: Captcha Recognition
        final boolean module_installed = new CaptchaRecognitionProxy(this).isModuleAvailable();
        Preference ext_captcha = fragment.findPreference("pref_extension_captcha_recognition");
        ext_captcha.setSummary(module_installed ? R.string.ext_installed : R.string.ext_not_installed);
        ext_captcha.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (module_installed) {
                    new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle(R.string.ext_captcha_recognition_title)
                            .setMessage(R.string.ext_captcha_recognition_summary)
                            .setPositiveButton(R.string.google_play, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(
                                            new Intent(SettingsActivity.this, SafeViewActivity.class)
                                                    .putExtra("data", getString(R.string.ext_captcha_recognition_link_google_play))
                                    );
                                }
                            })
                            .setNegativeButton(R.string.direct_link, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(
                                            new Intent(SettingsActivity.this, SafeViewActivity.class)
                                                    .putExtra("data", getString(R.string.ext_captcha_recognition_link_direct))
                                    );
                                }
                            })
                            .show();
                } else {
                    startActivity(new Intent(Intent.ACTION_DELETE)
                            .setData(Uri.parse("package:" + CaptchaRecognitionProxy.REMOTE_PACKAGE))
                    );
                }

                return false;
            }
        });
    }

    @RequiresApi(23)
    private void energy_saving_setup() {
        final PermissionUtils pu = new PermissionUtils(this);

        AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_battery_saving)
                .setMessage(R.string.dialog_battery_saving_summary)
                .setPositiveButton(R.string.permission_request, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pu.requestBatterySavingIgnore();
                    }
                })
                .setNeutralButton(R.string.open_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pu.openBatterySavingSettings();
                    }
                })
                .setNegativeButton(R.string.ignore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        settings.edit()
                                .putBoolean("pref_battery_saving_ignore", true)
                                .apply();
                        dialog.dismiss();
                    }
                });

        if (!settings.getBoolean("pref_battery_saving_ignore", false))
            if (!pu.isBatterySavingIgnored())
                dialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Populate preferences
        fragment = new SettingsFragment();
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit();
        getFragmentManager().executePendingTransactions();

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        downloader = new Downloader(this);

        // Add version name and code
        Preference app_name = fragment.findPreference("app_name");
        app_name.setSummary(getString(R.string.version, Version.getFormattedVersion()));

        // Start/stop service on pref_autoconnect change
        final CheckBoxPreference pref_autoconnect =
                (CheckBoxPreference) fragment.findPreference("pref_autoconnect");
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
        if (Build.VERSION.SDK_INT >= 23)
            energy_saving_setup();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        downloader.stop();
    }
}
