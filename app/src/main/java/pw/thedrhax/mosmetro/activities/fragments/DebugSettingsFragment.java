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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.util.Logger;

public class DebugSettingsFragment extends NestedFragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.pref_debug));
        addPreferencesFromResource(R.xml.pref_debug);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        Preference.OnPreferenceChangeListener reload_logger = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference pref, Object new_value) {
                settings.edit()
                        .putBoolean(pref.getKey(), (Boolean) new_value)
                        .apply();
                Logger.configure(getActivity());
                return true;
            }
        };

        CheckBoxPreference pref_debug_acra = (CheckBoxPreference)
                getPreferenceScreen().findPreference("acra.enable");
        CheckBoxPreference pref_debug_last_log = (CheckBoxPreference)
                getPreferenceScreen().findPreference("pref_debug_last_log");
        CheckBoxPreference pref_debug_testing = (CheckBoxPreference)
                getPreferenceScreen().findPreference("pref_debug_testing");
        CheckBoxPreference pref_debug_logcat = (CheckBoxPreference)
                getPreferenceScreen().findPreference("pref_debug_logcat");

        pref_debug_last_log.setEnabled(pref_debug_acra.isChecked());
        pref_debug_testing.setEnabled(pref_debug_last_log.isChecked());

        pref_debug_acra.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
            @Override
            public boolean onPreferenceChange(Preference pref, Object new_value) {
                if (!(Boolean)new_value) {
                    pref_debug_last_log.setChecked(false);
                    pref_debug_testing.setChecked(false);
                    pref_debug_testing.setEnabled(false);
                }

                pref_debug_last_log.setEnabled((Boolean) new_value);
                return true;
            }
        });

        pref_debug_last_log.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
            public boolean onPreferenceChange(Preference pref, Object new_value) {
                if (!(Boolean)new_value) {
                    pref_debug_testing.setChecked(false);
                }

                pref_debug_testing.setEnabled((Boolean) new_value);
                return true;
            };
        });

        pref_debug_testing.setOnPreferenceChangeListener(reload_logger);
        pref_debug_logcat.setOnPreferenceChangeListener(reload_logger);
    }
}
