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

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import androidx.annotation.Nullable;

import pw.thedrhax.mosmetro.R;

public class NotificationSettingsFragment extends NestedFragment {
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
