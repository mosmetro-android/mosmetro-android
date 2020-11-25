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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.acra.ACRA;

import java.util.Map;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.preferences.LoginFormPreference;
import pw.thedrhax.mosmetro.services.ConnectionService;
import pw.thedrhax.mosmetro.updater.UpdateCheckTask;
import pw.thedrhax.util.Listener;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.PermissionUtils;
import pw.thedrhax.util.Randomizer;
import pw.thedrhax.util.Version;

public class SettingsActivity extends AppCompatActivity {
    private SettingsFragment fragment;
    private Listener<Map<String,UpdateCheckTask.Branch>> branches;
    private SharedPreferences settings;

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    public static class NestedFragment extends PreferenceFragment {
        protected void setTitle(String title) {
            ActionBar bar = ((AppCompatActivity)getActivity()).getSupportActionBar();
            if (bar != null) bar.setTitle(title);
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public void onPrepareOptionsMenu(Menu menu) {
            super.onPrepareOptionsMenu(menu);
            menu.clear();
        }
    }

    public static class BranchFragment extends NestedFragment {
        private Map<String,UpdateCheckTask.Branch> branches;

        public BranchFragment branches(@NonNull Map<String, UpdateCheckTask.Branch> branches) {
            this.branches = branches; return this;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setTitle(getString(R.string.pref_updater_branch));

            PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getActivity());
            setPreferenceScreen(screen);

            PreferenceCategory stable = new PreferenceCategory(getActivity());
            stable.setTitle(R.string.pref_updater_branch_stable);
            screen.addPreference(stable);

            PreferenceCategory experimental = new PreferenceCategory(getActivity());
            experimental.setTitle(R.string.pref_updater_branch_experimental);
            screen.addPreference(experimental);

            if (branches == null) return;
            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
            for (final UpdateCheckTask.Branch branch : branches.values()) {
                CheckBoxPreference pref = new CheckBoxPreference(getActivity()) {
                    @Override
                    protected void onBindView(View view) {
                        super.onBindView(view);

                        // Increase number of lines on Android 4.x
                        // Source: https://stackoverflow.com/a/2615650
                        TextView summary = (TextView) view.findViewById(android.R.id.summary);
                        summary.setMaxLines(15);
                    }
                };
                pref.setTitle(branch.name);
                pref.setSummary(branch.description);
                pref.setChecked(Version.getBranch().equals(branch.name));
                pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        boolean same = Version.getBranch().equals(branch.name);
                        ((CheckBoxPreference)preference).setChecked(same);
                        if (!same) {
                            settings.edit().putInt("pref_updater_ignore", 0).apply();
                            branch.dialog().show();
                        }
                        getActivity().onBackPressed();
                        return true;
                    }
                });

                if (branch.stable) {
                    stable.addPreference(pref);
                } else {
                    experimental.addPreference(pref);
                }
            }
        }
    }

    public static class ConnectionSettingsFragment extends NestedFragment {
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Generate random User-Agent if it is unset
            new Randomizer(getActivity()).cached_useragent();

            setTitle(getString(R.string.pref_category_connection));
            addPreferencesFromResource(R.xml.pref_conn);

            PreferenceScreen screen = getPreferenceScreen();

            final CheckBoxPreference pref_mainet = (CheckBoxPreference)
                    screen.findPreference("pref_mainet");
            final LoginFormPreference pref_mainet_creds = (LoginFormPreference)
                    screen.findPreference("pref_mainet_credentials");
            pref_mainet_creds.setEnabled(pref_mainet.isChecked());
            pref_mainet.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    pref_mainet_creds.setEnabled((Boolean) newValue);
                    return true;
                }
            });
        }
    }

    public static class NotificationSettingsFragment extends NestedFragment {
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setTitle(getString(R.string.pref_category_notifications));
            addPreferencesFromResource(R.xml.pref_notify);

            PreferenceScreen screen = getPreferenceScreen();

            // Link pref_notify_foreground and pref_notify_success_lock
            final CheckBoxPreference foreground = (CheckBoxPreference)
                    screen.findPreference("pref_notify_foreground");
            final CheckBoxPreference success = (CheckBoxPreference)
                    screen.findPreference("pref_notify_success");
            final CheckBoxPreference success_lock = (CheckBoxPreference)
                    screen.findPreference("pref_notify_success_lock");
            foreground.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    success.setEnabled(!((Boolean) newValue));
                    success_lock.setEnabled(!((Boolean) newValue));
                    return true;
                }
            });
            foreground
                    .getOnPreferenceChangeListener()
                    .onPreferenceChange(foreground, foreground.isChecked());
        }
    }

    public static class DebugSettingsFragment extends NestedFragment {
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setTitle(getString(R.string.pref_debug));
            addPreferencesFromResource(R.xml.pref_debug);

            Preference.OnPreferenceChangeListener reload_logger = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    PreferenceManager.getDefaultSharedPreferences(getActivity())
                            .edit()
                            .putBoolean(preference.getKey(), (Boolean) newValue)
                            .apply();
                    Logger.configure(getActivity());
                    return true;
                }
            };

            CheckBoxPreference pref_debug_logcat =
                    (CheckBoxPreference) getPreferenceScreen().findPreference("pref_debug_logcat");
            pref_debug_logcat.setOnPreferenceChangeListener(reload_logger);
        }
    }

    public static class AboutFragment extends NestedFragment {
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setTitle(getString(R.string.about));
            addPreferencesFromResource(R.xml.about);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void donate_dialog() {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip;

                switch (i) {
                    case 0: // Yandex.Money
                        startActivity(new Intent(SettingsActivity.this, SafeViewActivity.class)
                                .putExtra("data", getString(R.string.donate_yandex_data))
                        );
                        break;

                    case 1: // Bitcoin
                        clip = ClipData.newPlainText("", getString(R.string.donate_bitcoin_data));
                        clipboard.setPrimaryClip(clip);

                        Toast.makeText(SettingsActivity.this,
                                R.string.clipboard_copy,
                                Toast.LENGTH_SHORT
                        ).show();
                        break;

                    case 2: // Ethereum
                        clip = ClipData.newPlainText("", getString(R.string.donate_ethereum_data));
                        clipboard.setPrimaryClip(clip);

                        Toast.makeText(SettingsActivity.this,
                                R.string.clipboard_copy,
                                Toast.LENGTH_SHORT
                        ).show();
                        break;

                    case 3: // Communities
                        replaceFragment("about", new AboutFragment());
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
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void update_checker_setup() {
        // Force check
        final Preference pref_updater_check = fragment.findPreference("pref_updater_check");
        pref_updater_check.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @SuppressLint("StaticFieldLeak")
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new UpdateCheckTask(SettingsActivity.this) {
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        pref_updater_check.setEnabled(false);
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        pref_updater_check.setEnabled(true);
                    }

                    @Override
                    public void result(@Nullable Map<String, Branch> result) {
                        branches.set(result);
                    }
                }.ignore(preference == null).force(preference != null).execute();
                return false;
            }
        });

        // Check for updates on start if enabled
        if (settings.getBoolean("pref_updater_enabled", true))
            pref_updater_check
                    .getOnPreferenceClickListener()
                    .onPreferenceClick(null);
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

    @RequiresApi(28)
    private void location_permission_setup() {
        final PermissionUtils pu = new PermissionUtils(this);

        AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.location_permission)
                .setMessage(R.string.location_permission_saving)
                .setPositiveButton(R.string.permission_request, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pu.requestCoarseLocation();
                    }
                })
                .setNeutralButton(R.string.open_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pu.openAppSettings();
                    }
                })
                .setNegativeButton(R.string.ignore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        settings.edit()
                                .putBoolean("pref_location_ignore", true)
                                .apply();
                        dialog.dismiss();
                    }
                });

        if (!settings.getBoolean("pref_location_ignore", false))
            if (!pu.isCoarseLocationGranted())
                dialog.show();
    }

    private void replaceFragment(String id, Fragment fragment) {
        try {
            getFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(id)
                    .commit();
        } catch (IllegalStateException ex) { // https://stackoverflow.com/q/7575921
            ACRA.getErrorReporter().handleException(ex);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Populate preferences
        final FragmentManager fmanager = getFragmentManager();
        fragment = new SettingsFragment();
        fmanager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit();
        fmanager.executePendingTransactions();
        fmanager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                boolean root = fmanager.getBackStackEntryCount() == 0;

                ActionBar bar = getSupportActionBar();
                if (bar != null) {
                    bar.setDisplayHomeAsUpEnabled(!root);

                    if (root) { // reset to defaults
                        bar.setTitle(R.string.app_name);
                    }
                }
            }
        });

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        // Hide shortcut button on Android 8+ (issue #211)
        if (Build.VERSION.SDK_INT >= 26) {
            Preference pref_shortcut = fragment.findPreference("pref_shortcut");
            fragment.getPreferenceScreen().removePreference(pref_shortcut);
        }

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

        // Branch Selector
        Preference pref_updater_branch = fragment.findPreference("pref_updater_branch");
        pref_updater_branch.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Map<String,UpdateCheckTask.Branch> branch_list = branches.get();
                if (branch_list != null) {
                    replaceFragment("branch", new BranchFragment().branches(branch_list));
                } else {
                    preference.setEnabled(false);
                }
                return true;
            }
        });

        branches = new Listener<Map<String,UpdateCheckTask.Branch>>(null) {
            @Override
            public void onChange(Map<String, UpdateCheckTask.Branch> new_value) {
                pref_updater_branch.setEnabled(new_value != null && new_value.size() > 0);
            }
        };

        // Connection Preferences
        Preference pref_conn = fragment.findPreference("pref_conn");
        pref_conn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                replaceFragment("conn", new ConnectionSettingsFragment());
                return true;
            }
        });

        // Notification Preferences
        Preference pref_notify = fragment.findPreference("pref_notify");
        pref_notify.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                replaceFragment("notify", new NotificationSettingsFragment());
                return true;
            }
        });

        // Debug
        Preference pref_debug = fragment.findPreference("pref_debug");
        pref_debug.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                replaceFragment("debug", new DebugSettingsFragment());
                return true;
            }
        });

        // About
        Preference pref_about = fragment.findPreference("pref_about");
        pref_about.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                replaceFragment("about", new AboutFragment());
                return true;
            }
        });

        update_checker_setup();
        if (Build.VERSION.SDK_INT >= 23)
            energy_saving_setup();
        if (Build.VERSION.SDK_INT >= 28)
            location_permission_setup();
    }
}
