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

import java.util.ArrayList;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.util.Util;

public class ThemeDialog extends AppCompatActivity {
    public static final int THEME_DEFAULT = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;
    public static final int THEME_OLED = 3;

    private static final ArrayList<Integer> OPTIONS = new ArrayList<Integer>() {{
        add(R.id.radio_default);
        add(R.id.radio_light);
        add(R.id.radio_dark);
        add(R.id.radio_oled);
    }};

    private SharedPreferences settings;
    private RadioGroup group;
    private int selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Util.getTheme(this, true));
        setContentView(R.layout.theme_dialog);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        selected = settings.getInt("pref_theme", 0);
        if (selected < 0 || selected > OPTIONS.size()) {
            selected = 0;
        }

        group = (RadioGroup) findViewById(R.id.radio_group);
        group.check(OPTIONS.get(selected));
    }

    public void button_cancel(View view) {
        finish();
    }

    public void button_save(View view) {
        int selection = OPTIONS.indexOf(group.getCheckedRadioButtonId());

        if (selection == selected) {
            finish();
            return;
        }

        settings.edit().putInt("pref_theme", selection).apply();

        Intent restart = new Intent(this, SettingsActivity.class);
        restart.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(restart);

        finish();
    }
}
