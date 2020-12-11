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

package pw.thedrhax.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.preferences.ThemeDialogPreferenceFragment;

public final class Util {
    private Util() {}

    // Source: https://stackoverflow.com/a/26779342
    public static int countLines(String input) {
        LineNumberReader reader = new LineNumberReader(new StringReader(input));
        try {
            reader.skip(Long.MAX_VALUE);
        } catch (IOException ignored) {}
        return reader.getLineNumber();
    }

    // TODO: Store Integers instead of Strings in SharedPreferences
    public static int getIntPreference (Context context, String name, int def_value) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return Integer.parseInt(settings.getString(name, Integer.valueOf(def_value).toString()));
        } catch (NumberFormatException|ClassCastException ignored) {}

        try {
            return settings.getInt(name, def_value);
        } catch (ClassCastException ignored) {}

        return def_value;
    }

    // Source: https://stackoverflow.com/a/34836992
    public static String readAsset(Context context, String filename) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(filename), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String mLine = reader.readLine();
        while (mLine != null) {
            sb.append(mLine).append('\n');
            mLine = reader.readLine();
        }
        reader.close();
        return sb.toString();
    }

    public static int getTheme(Context context, boolean dialog) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        int theme = settings.getInt("pref_theme", 0);

        if (theme == ThemeDialogPreferenceFragment.THEME_DARK || theme == ThemeDialogPreferenceFragment.THEME_OLED) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (theme == ThemeDialogPreferenceFragment.THEME_LIGHT) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }

        if (!dialog) {
            if (theme == ThemeDialogPreferenceFragment.THEME_OLED) {
                return R.style.AppBaseTheme_OLED;
            } else {
                return R.style.AppBaseTheme;
            }
        } else {
            if (theme == ThemeDialogPreferenceFragment.THEME_OLED) {
                return R.style.AppBaseTheme_OLED_Dialog;
            } else {
                return R.style.AppBaseTheme_Dialog;
            }
        }
    }
}
