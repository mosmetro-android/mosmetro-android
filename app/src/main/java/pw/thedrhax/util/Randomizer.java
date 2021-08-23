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

import java.security.SecureRandom;

/**
 * This class is used to get random values and delays.
 */
public class Randomizer {
    private Context context;
    private SecureRandom random = new SecureRandom();

    private int delay_min = 0;
    private int delay_max = 0;

    public Randomizer(Context context) {
        this.context = context;

        delay_min = Util.getIntPreference(context, "pref_delay_range_min", 500);
        delay_max = Util.getIntPreference(context, "pref_delay_range_max", 1000);

        if (delay_min > delay_max) {
            int temp = delay_max;
            delay_max = delay_min;
            delay_min = temp;
        }
    }

    public boolean delay(Listener<Boolean> running) {
        double a = (delay_max + delay_min) / 2;
        double s = Math.sqrt(delay_max - a) / 3;
        int delay = (int) (random.nextGaussian() * s + a);
        return running.sleep(delay);
    }

    // grep download.php access.log | grep -Eo '(Mozilla|Opera)[^"]*' | \
    //     sed 's/(.*) \(Gecko\|AppleWebKit\)/(DEVICE) \1/g' | \
    //     sort -u | grep DEVICE | sed 's/^.*$/"\0",/'
    private static final String[] useragents = new String[] {
            "Mozilla/5.0 (DEVICE) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/61.0.3163.128 Safari/534.24 XiaoMi/MiuiBrowser/9.8.4",
            "Mozilla/5.0 (DEVICE) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Safari/534.30",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.22 (KHTML, like Gecko) Chrome/25.0.1364.172 YaBrowser/1.20.1364.172 Mobile Safari/537.22",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 YaBrowser/16.6.0.8810.00 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.98 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.91 Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.82 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.107 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.9 Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 YaBrowser/17.11.0.530.00 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.109 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.158 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.158 Mobile Safari/537.36 OPR/47.1.2249.129326",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.158 Mobile Safari/537.36 OPR/47.2.2249.130418",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.158 Safari/537.36 OPR/47.1.2249.129326",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.103 YaBrowser/18.7.0.823.00 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.103 YaBrowser/18.7.0.823.01 Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.103 YaBrowser/18.7.1.575.00 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.87 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.91 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.91 Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.76 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3535.2 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.2 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/7.2 Chrome/59.0.3071.125 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/7.4 Chrome/59.0.3071.125 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/8.0 Chrome/63.0.3239.111 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/1.6 Chrome/28.0.1500.94 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/40.0.2214.89 MZBrowser/7.1.110-2018072414 UWS/2.11.0.33 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/45.0.2454.95 Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/49.0.2623.108 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/50.0.2661.86 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/53.0.2785.146 Mobile Safari/537.36 XiaoMi/MiuiBrowser/8.7.7",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/53.0.2785.146 Mobile Safari/537.36 XiaoMi/MiuiBrowser/9.5.6",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/60.0.3112.107 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.3163.128 Mobile Safari/537.36 XiaoMi/MiuiBrowser/9.7.3",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.3163.128 Mobile Safari/537.36 XiaoMi/MiuiBrowser/9.8.4",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.3163.128 Mobile Safari/537.36 XiaoMi/MiuiBrowser/9.8.5",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/65.0.3325.109 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/67.0.3396.87 Mobile Safari/537.36 YandexSearch/7.70 YandexSearchBrowser/7.70",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/68.0.3440.91 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/68.0.3440.91 Mobile Safari/537.36 OPR/36.2.2254.130496",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/68.0.3440.91 YaBrowser/18.4.2.100 (lite) Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Focus/6.1.1 Chrome/68.0.3440.91 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) Gecko/56.0 Firefox/56.0",
            "Mozilla/5.0 (DEVICE) Gecko/61.0 Firefox/61.0",
            "Mozilla/5.0 (DEVICE) Gecko/62.0 Firefox/62.0"
    };

    private String useragent() {
        String ua = System.getProperty("http.agent", "()");
        String device = ua.substring(ua.indexOf("("), ua.indexOf(")") + 1);
        ua = (String)choose(useragents);
        ua = ua.replace("(DEVICE)", device);
        return ua;
    }

    public String cached_useragent() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        if ("unset".equals(settings.getString("pref_user_agent", "unset")))
            settings.edit()
                    .putString("pref_user_agent", useragent())
                    .apply();

        return settings.getString("pref_user_agent", useragent());
    }

    public String string(String chars, int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(chars.charAt(random.nextInt(chars.length())));
        }
        return builder.toString();
    }

    public String string(int length) {
        return string("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", length);
    }

    public Object choose(Object[] array) {
        return array[(int) Math.floor(random.nextInt(array.length))];
    }
}
