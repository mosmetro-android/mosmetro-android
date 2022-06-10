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

    // grep 'HTTP/1.1" 2' log \
    //     | grep -E '(Android|Mobile)' \
    //     | cut -d\  -f 12- \
    //     | sed -e 's/^\("[^"]*"\).*$/\1",/' -e 's/([^)]*)/(DEVICE)/' \
    //     | sort -u
    private static final String[] useragents = new String[] {
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Mobile Safari/537.36 OPR/69.2.3606.65175",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Mobile Safari/537.36 OPR/69.3.3606.65458",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.143 YaBrowser/22.5.1.156.00 SA/3 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.160 YaApp_Android/22.53.1 YaSearchBrowser/22.53.1 BroPP/1.0 SA/3 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.160 YaApp_Android/22.54.1 YaSearchBrowser/22.54.1 BroPP/1.0 SA/3 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.160 YaBrowser/22.5.3.85.00 SA/3 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.160 YaBrowser/22.5.4.84.00 SA/3 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.160 YaBrowser/22.5.4.84.01 Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.79 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.61 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.115 Mobile Safari/537.36 PTST/220609.133020",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.78 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.78 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.78 Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.98 Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.99 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.116 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.89 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 YaBrowser/20.11.1.100.00 SA/3 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.101 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.210 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.216 YaBrowser/21.5.4.119.00 SA/3 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.164 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.77 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.105 HuaweiBrowser/12.0.4.304 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.75 Mobile Safari/537.36 ABB/3.1.1-beta1",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.104 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.98 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4695.0 Mobile Safari/537.36 Chrome-Lighthouse",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.132 YaBrowser/22.3.3.91.00 SA/3 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.73 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.88 Mobile Safari/537.36 OPR/68.2.3557.64219",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/11.0 Chrome/75.0.3770.143 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/14.2 Chrome/87.0.4280.141 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/16.2 Chrome/92.0.4515.166 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/17.0 Chrome/96.0.4664.104 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/17.0 Chrome/96.0.4664.104 Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.3163.128 Mobile Safari/537.36 XiaoMi/MiuiBrowser/10.1.1",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/89.0.4389.116 Mobile Safari/537.36 XiaoMi/MiuiBrowser/13.5.1-gn",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/89.0.4389.116 Mobile Safari/537.36 XiaoMi/MiuiBrowser/13.6.0-gn",
            "Mozilla/5.0 (DEVICE) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1",
            "Mozilla/5.0 (DEVICE) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/102.0.5005.87 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (DEVICE) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/96.0.4664.116 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (DEVICE) AppleWebKit/605.1.15 (KHTML, like Gecko) GSA/213.0.449417121 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (DEVICE) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 OPT/3.2.17",
            "Mozilla/5.0 (DEVICE) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (DEVICE) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0 YaBrowser/22.1.3.586.10 SA/3 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (DEVICE) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.2 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (DEVICE) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (DEVICE) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.2 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (DEVICE) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 YaBrowser/22.5.2.374.11 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (DEVICE) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.3 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (DEVICE) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (DEVICE) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (DEVICE) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (DEVICE) Gecko/101.0 Firefox/101.0",
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
