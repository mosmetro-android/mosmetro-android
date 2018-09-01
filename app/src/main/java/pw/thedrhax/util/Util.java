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
import android.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class Util {
    private Util() {}

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
                new InputStreamReader(context.getAssets().open(filename), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String mLine = reader.readLine();
        while (mLine != null) {
            sb.append(mLine).append('\n');
            mLine = reader.readLine();
        }
        reader.close();
        return sb.toString();
    }
}
