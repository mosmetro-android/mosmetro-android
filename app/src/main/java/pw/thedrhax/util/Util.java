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

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.Option;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;

public final class Util {
    private Util() {}

    public static InetAddress intToAddr(int hex) throws UnknownHostException {
        byte[] b = new byte[4];

        b[3] = (byte) ((hex & 0xFF000000) >> 24);
        b[2] = (byte) ((hex & 0x00FF0000) >> 16);
        b[1] = (byte) ((hex & 0x0000FF00) >> 8);
        b[0] = (byte) (hex & 0x000000FF);

        return InetAddress.getByAddress(b);
    }

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

    public final static Configuration JSONPATH_CONFIG = Configuration
            .defaultConfiguration()
            .addOptions(
                    Option.SUPPRESS_EXCEPTIONS,
                    Option.DEFAULT_PATH_LEAF_TO_NULL
            );

    public final static DocumentContext JSONPATH_EMPTY = JsonPath.using(JSONPATH_CONFIG).parse("{}");

    public static DocumentContext jsonpath(String json) throws ParseException {
        try {
            DocumentContext data = JsonPath.using(JSONPATH_CONFIG).parse(json);
            data.jsonString(); // throws UnsupportedOperationException
            return data;
        } catch (IllegalArgumentException|JsonPathException|UnsupportedOperationException ex) {
            throw new ParseException(ex.toString(), 0);
        }
    }


}
