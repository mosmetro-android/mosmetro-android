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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
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
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.acra.ACRA;

import java.util.Map;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.fragments.AboutFragment;
import pw.thedrhax.mosmetro.activities.fragments.BranchFragment;
import pw.thedrhax.mosmetro.activities.fragments.ConnectionSettingsFragment;
import pw.thedrhax.mosmetro.activities.fragments.DebugSettingsFragment;
import pw.thedrhax.mosmetro.activities.fragments.NotificationSettingsFragment;
import pw.thedrhax.mosmetro.activities.fragments.SettingsFragment;
import pw.thedrhax.mosmetro.services.ConnectionService;
import pw.thedrhax.mosmetro.services.ReceiverService;
import pw.thedrhax.mosmetro.updater.UpdateChecker;
import pw.thedrhax.util.Listener;
import pw.thedrhax.util.PermissionUtils;
import pw.thedrhax.util.Version;

public class SettingsActivity extends Activity {
    private SettingsFragment fragment;
    private PermissionUtils pu;
    private Listener<Map<String, UpdateChecker.Branch>> branches;
    private SharedPreferences settings;
    private CheckBoxPreference pref_autoconnect;
    private CheckBoxPreference pref_autoconnect_service;

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
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip;

                switch (i) {
                    case 0: // Yandex.Money / YooMoney
                        startActivity(new Intent(SettingsActivity.this, SafeViewActivity.class)
                                .putExtra("data", getString(R.string.donate_yandex_data))
                        );
                        break;

                    case 1: // QIWI Wallet
                        startActivity(new Intent(SettingsActivity.this, SafeViewActivity.class)
                                .putExtra("data", getString(R.string.donate_qiwi_data))
                        );
                        break;

                    case 2: // Sberbank
                        clip = ClipData.newPlainText("", getString(R.string.donate_sberbank_data));
                        clipboard.setPrimaryClip(clip);

                        Toast.makeText(SettingsActivity.this,
                                R.string.clipboard_copy,
                                Toast.LENGTH_SHORT
                        ).show();
                        break;

                    case 3: // Ethereum
                        clip = ClipData.newPlainText("", getString(R.string.donate_ethereum_data));
                        clipboard.setPrimaryClip(clip);

                        Toast.makeText(SettingsActivity.this,
                                R.string.clipboard_copy,
                                Toast.LENGTH_SHORT
                        ).show();
                        break;

                    case 4: // Communities
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
        final Preference pref_updater_check = fragment.findPreference("pref_updater_check");

        Preference.OnPreferenceClickListener click = new Preference.OnPreferenceClickListener() {
            @SuppressLint("StaticFieldLeak")
            @Override
            public boolean onPreferenceClick(Preference preference) {
                boolean manual = preference != null;

                UpdateChecker updater = new UpdateChecker(SettingsActivity.this)
                        .ignore(!manual).force(manual);

                updater.async_check(new UpdateChecker.Callback() {
                    @Override
                    public void onStart() {
                        pref_updater_check.setEnabled(false);
                    }

                    @Override
                    public void onResult(UpdateChecker.Result result) {
                        pref_updater_check.setEnabled(true);
                        branches.set(result.getBranches());

                        if (result != null && (result.hasUpdate() || manual)) {
                            result.showDialog();
                        }
                    }
                });

                return false;
            }
        };

        // Force check
        pref_updater_check.setOnPreferenceClickListener(click);

        // Check for updates on start if enabled
        if (settings.getBoolean("pref_updater_enabled", true))
            click.onPreferenceClick(null);
    }

    @RequiresApi(23)
    private void energy_saving_setup() {
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

    private void changeCheckBox(CheckBoxPreference pref, boolean checked) {
        pref.setChecked(checked);
        pref.getOnPreferenceChangeListener().onPreferenceChange(pref, checked);
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    @RequiresApi(28)
    private void showLocationDialog(boolean coarse) {
        new AlertDialog.Builder(this).setTitle(R.string.warning)
                .setMessage(coarse ? getString(R.string.coarse_location_permission)
                        : getString(R.string.background_location_permission))
                .setNegativeButton(coarse ? getString(R.string.force_on) : getString(R.string.force_off),
                        (dialogInterface, i) -> {
                            if (coarse) {
                                settings.edit().putBoolean("pref_location_ignore_autoconnect", true).apply();

                                changeCheckBox(pref_autoconnect, true);
                            } else {
                                settings.edit().putBoolean("pref_location_ignore_autoconnect_service", true).apply();

                                changeCheckBox(pref_autoconnect_service, false);
                            }
                        })
                .setPositiveButton(R.string.permission_request, (dialogInterface, i) -> {
                    permissionRequestTimestamp = System.currentTimeMillis();

                    if (coarse || Build.VERSION.SDK_INT < 29) {
                        pu.requestCoarseLocation(SettingsActivity.this, 1);
                    } else {
                        pu.requestBackgroundLocation(SettingsActivity.this, 2);
                    }
                })
                .setCancelable(true)
                .show();
    }

    private long permissionRequestTimestamp = 0;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Request codes:
        // 0 - from starting dialog
        // 1 - requesting coarse location by enabling pref_autoconnect
        // 2 - requesting background location by disabling pref_autoconnect_service

        // Approved or rejected automatically (too fast for user)
        boolean auto = System.currentTimeMillis() - permissionRequestTimestamp < 200;

        if (auto && (requestCode == 1 || requestCode == 2)) {
            openAppSettings();
        }

        if (permissions.length == 0 || grantResults.length == 0) return;

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (permissions[0]) {
                case Manifest.permission.ACCESS_BACKGROUND_LOCATION:
                case Manifest.permission.ACCESS_COARSE_LOCATION:
                    if (Build.VERSION.SDK_INT >= 29 && pu.isBackgroundLocationGranted()) {
                        changeCheckBox(pref_autoconnect_service, false);
                    }

                    changeCheckBox(pref_autoconnect, true);
            }
        } else {
            switch (permissions[0]) {
                case Manifest.permission.ACCESS_COARSE_LOCATION:
                    changeCheckBox(pref_autoconnect, false);
                case Manifest.permission.ACCESS_BACKGROUND_LOCATION:
                    changeCheckBox(pref_autoconnect_service, true);
            }
        }
    }

    @RequiresApi(28)
    private void location_permission_setup() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.location_permission)
                .setMessage(R.string.location_permission_saving)
                .setPositiveButton(R.string.permission_request, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pu.requestCoarseLocation(SettingsActivity.this, 0);
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

        if (!settings.getBoolean("pref_location_ignore_autoconnect", false)
                && !pu.isCoarseLocationGranted()
                && pref_autoconnect.isChecked()) {
            changeCheckBox(pref_autoconnect, false);
        }

        if (!settings.getBoolean("pref_location_ignore_autoconnect_service", false)
                && Build.VERSION.SDK_INT >= 29
                && !pu.isBackgroundLocationGranted()
                && !pref_autoconnect_service.isChecked()) {
            changeCheckBox(pref_autoconnect_service, true);
        }
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
        pu = new PermissionUtils(this);

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

                ActionBar bar = getActionBar();
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
        pref_autoconnect = (CheckBoxPreference) fragment.findPreference("pref_autoconnect");
        pref_autoconnect_service = (CheckBoxPreference) fragment.findPreference("pref_autoconnect_service");

        Intent receiver_service = new Intent(SettingsActivity.this, ReceiverService.class);

        pref_autoconnect.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object new_value) {
                boolean force = settings.getBoolean("pref_location_ignore_autoconnect", false);

                if (!force && (Boolean)new_value && Build.VERSION.SDK_INT >= 28 && !pu.isCoarseLocationGranted()) {
                    showLocationDialog(true);
                    return false;
                }

                Intent service = new Intent(SettingsActivity.this, ConnectionService.class);
                if (!(Boolean)new_value) {
                    service.setAction(ConnectionService.ACTION_STOP);
                }
                startService(service);

                if (pref_autoconnect_service.isChecked() && (Boolean)new_value) {
                    startService(receiver_service);
                } else {
                    settings.edit()
                            .putBoolean("pref_location_ignore_autoconnect", false)
                            .apply();

                    stopService(receiver_service);
                }

                pref_autoconnect_service.setEnabled((Boolean)new_value);

                return true;
            }
        });

        pref_autoconnect_service.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
            @Override
            public boolean onPreferenceChange(Preference preference, Object new_value) {
                boolean force = settings.getBoolean("pref_location_ignore_autoconnect_service", false);

                if (!force && !(Boolean)new_value && Build.VERSION.SDK_INT >= 29 && !pu.isBackgroundLocationGranted()) {
                    showLocationDialog(false);
                    return false;
                }

                if (pref_autoconnect.isChecked() && (Boolean)new_value) {
                    settings.edit()
                            .putBoolean("pref_location_ignore_autoconnect_service", false)
                            .apply();

                    startService(receiver_service);
                } else {
                    stopService(receiver_service);
                }

                return true;
            }
        });

        if (!pref_autoconnect.isChecked()) {
            pref_autoconnect_service.setEnabled(false);
        }

        if (pref_autoconnect.isChecked() && pref_autoconnect_service.isChecked()) {
            startService(receiver_service);
        }

        // Branch Selector
        Preference pref_updater_branch = fragment.findPreference("pref_updater_branch");
        pref_updater_branch.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Map<String, UpdateChecker.Branch> branch_list = branches.get();
                if (branch_list != null) {
                    replaceFragment("branch", new BranchFragment().branches(branch_list));
                } else {
                    preference.setEnabled(false);
                }
                return true;
            }
        });

        branches = new Listener<Map<String, UpdateChecker.Branch>>(null) {
            @Override
            public void onChange(Map<String, UpdateChecker.Branch> new_value) {
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
