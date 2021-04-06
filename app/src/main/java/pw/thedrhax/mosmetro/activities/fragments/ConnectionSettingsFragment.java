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
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import androidx.annotation.Nullable;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.preferences.LoginFormPreference;
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
    }
}
