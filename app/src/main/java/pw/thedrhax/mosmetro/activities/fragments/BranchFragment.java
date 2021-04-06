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

import java.util.Map;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.updater.UpdateChecker;
import pw.thedrhax.util.Version;

public class BranchFragment extends NestedFragment {
    private Map<String, UpdateChecker.Branch> branches;

    public BranchFragment branches(@NonNull Map<String, UpdateChecker.Branch> branches) {
        this.branches = branches;
        return this;
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

        if (branches == null)
            return;
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        for (final UpdateChecker.Branch branch : branches.values()) {
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
                    ((CheckBoxPreference) preference).setChecked(same);
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
