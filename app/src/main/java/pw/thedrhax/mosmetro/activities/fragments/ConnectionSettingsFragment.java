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

package pw.thedrhax.mosmetro.activities.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import androidx.annotation.Nullable;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.SafeViewActivity;
import pw.thedrhax.mosmetro.activities.SettingsActivity;
import pw.thedrhax.mosmetro.preferences.LoginFormPreference;
import pw.thedrhax.util.CaptivePortalFix;
import pw.thedrhax.util.Randomizer;

public class ConnectionSettingsFragment extends NestedFragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Generate random User-Agent if it is unset
        new Randomizer(getActivity()).cached_useragent();

        setTitle(getString(R.string.pref_category_connection));
        addPreferencesFromResource(R.xml.pref_conn);

        PreferenceScreen screen = getPreferenceScreen();

        final CheckBoxPreference mmv2 = (CheckBoxPreference)
                screen.findPreference("pref_mosmetro_v2_wv");
        mmv2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (mmv2.isChecked()) {
                    mmv2.setChecked(false);

                    final AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.warning)
                            .setMessage(R.string.pref_mosmetro_v2_wv_warning)
                            .setPositiveButton("Включить", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    mmv2.setChecked(true);
                                }
                            })
                            .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            })
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialogInterface) {
                                    mmv2.setChecked(false);
                                }
                            })
                            .setCancelable(true);

                    dialog.show();
                    return false;
                }

                return true;
            }
        });

        // Midsession fix
        CaptivePortalFix cpf = new CaptivePortalFix(getActivity());
        CheckBoxPreference pref_captive_fix = (CheckBoxPreference) screen.findPreference("pref_captive_fix");
        CheckBoxPreference pref_internet_midsession = (CheckBoxPreference) screen.findPreference("pref_internet_midsession");

        boolean isApplied = cpf.isApplied();
        pref_captive_fix.setChecked(cpf.isApplied());

        if (isApplied) {
            pref_internet_midsession.setChecked(false);
            pref_internet_midsession.setEnabled(false);
        }

        pref_captive_fix.setOnPreferenceClickListener(preference -> {
            cpf.toggle(result -> {
                boolean applied = cpf.isApplied();

                if (applied) pref_internet_midsession.setChecked(false);
                pref_internet_midsession.setEnabled(!applied);
                pref_captive_fix.setChecked(applied);

                if (!result.isSuccess()) {
                    showCaptivePortalFixDialog();
                }
            });

            return true;
        });
    }

    private void showCaptivePortalFixDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.root_unavailable)
                .setMessage(R.string.captive_fix_message)
                .setPositiveButton(R.string.captive_fix_open_manual, (dialog, which) -> {
                        getActivity().startActivity(
                                new Intent(getActivity(), SafeViewActivity.class)
                                        .putExtra("data", getString(R.string.captive_fix_manual_url))
                        );
                })
                .setNegativeButton(R.string.cancel, ((dialog, which) -> dialog.dismiss()))
                .show();
    }
}
