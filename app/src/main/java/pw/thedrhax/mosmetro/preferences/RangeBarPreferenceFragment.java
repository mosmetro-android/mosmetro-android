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

package pw.thedrhax.mosmetro.preferences;

import com.edmodo.rangebar.RangeBar;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceManager;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.util.Util;

public class RangeBarPreferenceFragment extends PreferenceDialogFragmentCompat {
    private SharedPreferences settings;
    private RangeBar rangebar;

    private int defaultMin, defaultMax, min, max;
    private final String key_min, key_max;

    public RangeBarPreferenceFragment(RangeBarPreference preference) {
        Bundle bundle = new Bundle(1);
        bundle.putString("key", preference.getKey());
        setArguments(bundle);

        key_min = preference.getKey() + "_min";
        key_max = preference.getKey() + "_max";
        defaultMin = preference.getDefaultMin();
        defaultMax = preference.getDefaultMax();
        min = preference.getMin();
        max = preference.getMax();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    @Override
    public View onCreateDialogView(Context context) {
        View view = getLayoutInflater().inflate(R.layout.rangebar_preference, null);
        rangebar = (RangeBar) view.findViewById(R.id.rangebar);
        
        rangebar.setTickCount(max - min + 1);
        rangebar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            private TextView rangetext = (TextView) view.findViewById(R.id.rangetext);

            @Override
            public void onIndexChangeListener(RangeBar rangeBar, int left, int right) {
                if (left < min || left > max) {
                    rangeBar.setLeft(min); return;
                }

                if (right < min || right > max) {
                    rangeBar.setRight(max); return;
                }

                rangetext.setText("" + (left + min) + " - " + (right + min));
            }
        });

        int current_min = Util.getIntPreference(getContext(), key_min, defaultMin);
        if (current_min < min)
            current_min = min;

        int current_max = Util.getIntPreference(getContext(), key_max, defaultMax);
        if (current_max > max)
            current_max = max;

        rangebar.setThumbIndices(current_min, current_max);

        return view;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            settings.edit()
                    .putInt(key_min, rangebar.getLeftIndex() + min)
                    .putInt(key_max, rangebar.getRightIndex() + min)
                    .apply();
        }
    }
}
