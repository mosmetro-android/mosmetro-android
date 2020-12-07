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

import android.content.Context;
import android.content.res.TypedArray;
import androidx.preference.DialogPreference;
import android.util.AttributeSet;

import pw.thedrhax.mosmetro.R;

public class RangeBarPreference extends DialogPreference {

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

    public RangeBarPreference(Context context) {
        super(context);
    }

    private int defaultMin, defaultMax, min, max;

    public int getDefaultMin() {
        return defaultMin;
    }

    public int getDefaultMax() {
        return defaultMax;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RangeBarPreference, 0, 0);

        defaultMin = ta.getInt(R.styleable.RangeBarPreference_defaultMin, 1);
        defaultMax = ta.getInt(R.styleable.RangeBarPreference_defaultMax, 10);
        min = ta.getInt(R.styleable.RangeBarPreference_min, 1);
        max = ta.getInt(R.styleable.RangeBarPreference_max, 10);

        ta.recycle();
    }
}
