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

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.Menu;
import android.widget.ListView;

import androidx.annotation.Nullable;

public class NestedFragment extends PreferenceFragment {
    public static final String ARG_SCROLL_TO = "scrollTo";

    protected void setTitle(String title) {
        ActionBar bar = getActivity().getActionBar();
        if (bar != null)
            bar.setTitle(title);
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

    @Override
    public void onStart() {
        super.onStart();

        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_SCROLL_TO)) {
            Preference scrollTo = findPreference(args.getString(ARG_SCROLL_TO));
            ListView list = getView().findViewById(android.R.id.list);

            if (list != null && scrollTo != null)
                list.setSelection(scrollTo.getOrder());
        }
    }
}
