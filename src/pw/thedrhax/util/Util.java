package pw.thedrhax.util;

import android.content.SharedPreferences;

public class Util {

    // TODO: Store Integers instead of Strings in SharedPreferences
    public static int getIntPreference (SharedPreferences settings, String name, int def_value) {
        int result = def_value;
        try {
            result = Integer.parseInt(settings.getString(name, Integer.valueOf(def_value).toString()));
        } catch (NumberFormatException ignored) {}
        return result;
    }
}
