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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.edmodo.rangebar.RangeBar;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.util.Util;

public class RangeBarPreference extends DialogPreference {

    @TargetApi(21)
    public RangeBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public RangeBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public RangeBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    @TargetApi(21)
    public RangeBarPreference(Context context) {
        super(context);
    }

    private String key_min = getKey() + "_min";
    private String key_max = getKey() + "_max";

    private int defaultMin, defaultMax, min, max;

    private void init(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RangeBarPreference, 0, 0);

        defaultMin = ta.getInt(R.styleable.RangeBarPreference_defaultMin, 1);
        defaultMax = ta.getInt(R.styleable.RangeBarPreference_defaultMax, 10);
        min = ta.getInt(R.styleable.RangeBarPreference_min, 1);
        max = ta.getInt(R.styleable.RangeBarPreference_max, 10);

        ta.recycle();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        final View view = View.inflate(getContext(), R.layout.rangebar_preference, null);
        final RangeBar rangebar = (RangeBar) view.findViewById(R.id.rangebar);

        rangebar.setTickCount(max - min + 1);
        rangebar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            private TextView rangetext = (TextView) view.findViewById(R.id.rangetext);

            @Override
            public void onIndexChangeListener(RangeBar rangeBar, int left, int right) {
                if (left < min)
                    rangeBar.setLeft(min);
                else if (right > max)
                    rangeBar.setRight(max);
                else
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

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                settings.edit()
                        .putInt(key_min, rangebar.getLeftIndex() + min)
                        .putInt(key_max, rangebar.getRightIndex() + min)
                        .apply();
            }
        });

        builder.setView(view);
    }
}
