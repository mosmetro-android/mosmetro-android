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
import android.os.SystemClock;
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

        delay_min = Util.getIntPreference(context, "pref_delay_range_min", 5);
        delay_max = Util.getIntPreference(context, "pref_delay_range_max", 10);
        if (delay_min > delay_max) { // User tries to break everything again...
            int temp = delay_max;
            delay_max = delay_min;
            delay_min = temp;
        }
    }

    public void delay(Listener<Boolean> running) {
        double a = (delay_max + delay_min) / 2;
        double s = Math.sqrt((delay_max - a)) / 3;
        int delay = (int) ((random.nextGaussian() * s + a) * 1000);

        while (delay >= 100 && running.get()) {
            delay -= 100;
            SystemClock.sleep(100);
        }

        if (delay > 0 && running.get()) {
            SystemClock.sleep(delay);
        }
    }

    private static final String[] useragents = new String[] {
            "Mozilla/5.0/5.1.1 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/45.0.2454.94 Mobile Safari/537.36",
            "Mozilla/5.0/5.1 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/45.0.2454.94 Mobile Safari/537.36",
            "Mozilla/5.0/6.0.1 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/45.0.2454.94 Mobile Safari/537.36",
            "Mozilla/5.0/6.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/45.0.2454.94 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Chrome/36.0.1985.135 Mobile Safari/534.30",
            "Mozilla/5.0 (DEVICE) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
            "Mozilla/5.0 (DEVICE) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30 ACHEETAHI/1",
            "Mozilla/5.0 (DEVICE) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Safari/534.30",
            "Mozilla/5.0 (DEVICE) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 UCBrowser/11.2.0.915 U3/0.8.0 Mobile Safari/534.30",
            "Mozilla/5.0 (DEVICE) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 UCBrowser/11.3.8.976 U3/0.8.0 Mobile Safari/534.30",
            "Mozilla/5.0 (DEVICE) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 UCBrowser/11.4.2.995 U3/0.8.0 Mobile Safari/534.30",
            "Mozilla/5.0 (DEVICE) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 UCBrowser/11.4.5.1005 U3/0.8.0 Mobile Safari/534.30",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.114 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.141 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.122 YaBrowser/14.12.2125.9740.00 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.109 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Mobile Safari/537.36 Puffin/6.1.4.16005AP",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.154 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.76 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.110 YaBrowser/16.4.0.9477.00 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.89 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.94 Mobile Safari/537.36 OPR/37.0.2192.105088",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 YaBrowser/16.7.0.2777.00 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.81 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.98 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.116 YaBrowser/16.10.2.1487.00 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.124 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.85 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.85 Mobile Safari/537.36 OPR/41.2.2246.111806",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.91 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.91 Mobile Safari/537.36 OPR/42.1.2246.112788",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.91 Mobile Safari/537.36 OPR/42.7.2246.114995",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.91 Mobile Safari/537.36 OPR/42.7.2246.114996",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.91 Mobile Safari/537.36 OPR/42.9.2246.119945",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.132 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.137 YaBrowser/17.4.0.544.00 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.137 YaBrowser/17.4.1.356.00 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 YaBrowser/17.6.0.312.00 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 YaBrowser/17.6.1.323.00 (beta) Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 YaBrowser/17.6.1.345.00 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.83 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.83 Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.125 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.125 Mobile Safari/537.36 OPR/43.0.2246.121128",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.125 Mobile Safari/537.36 OPR/43.0.2246.121183",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.125 YaBrowser/17.7.0.1173.00 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.125 YaBrowser/17.7.0.1178.00 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.125 YaBrowser/17.7.0.1178.01 Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.107 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 YaBrowser/17.9.0.481.00 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 YaBrowser/17.9.0.481.01 Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 YaBrowser/17.9.0.500.00 (beta) Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 YaBrowser/17.9.0.513.00 (beta) Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 YaBrowser/17.9.1.389.00 (alpha) Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 YaBrowser/17.9.1.389.01 (alpha) Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.116 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.78 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 YaBrowser/17.10.0.340.00 (alpha) Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.98 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.98 Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.38 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.45 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3219.0 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3233.3 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3234.0 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/2.0 Chrome/34.0.1847.76 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/3.3 Chrome/38.0.2125.102 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/3.5 Chrome/38.0.2125.102 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/4.2 Chrome/44.0.2403.133 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/5.2 Chrome/51.0.2704.106 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/5.4 Chrome/51.0.2704.106 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/6.0 Chrome/56.0.2924.87 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/6.2 Chrome/56.0.2924.87 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/1.5 Chrome/28.0.1500.94 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/1.6 Chrome/28.0.1500.94 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36;",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36 ACHEETAHI/1",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36 Mobile UCBrowser/3.4.3.532",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36 MxBrowser/4.5.10.9000",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/33.0.0.0 Safari/537.36 Mobile UCBrowser/3.4.3.532",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/37.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/38.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/38.0.2125.102 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/39.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/39.0.0.0 Mobile Safari/537.36 ACHEETAHI/1",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/40.0.2214.89 MZBrowser/6.6.101 UWS/2.10.1.22 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/40.0.2214.89 UCBrowser/11.4.6.1017 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/43.0.2357.121 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/44.0.2403.119 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/44.0.2403.119 Mobile Safari/537.36 ACHEETAHI/1",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/45.0.2454.94 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/46.0.2490.85 Mobile Safari/537.36 XiaoMi/MiuiBrowser/8.1.4",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/46.0.2490.85 Mobile Safari/537.36 XiaoMi/MiuiBrowser/8.4.3",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko)Version/4.0 Chrome/49.0.0.0 Mobile Safari/537.36 EUI Browser/5.8.018S",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko)Version/4.0 Chrome/49.0.0.0 Mobile Safari/537.36 EUI Browser/5.8.019S",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/49.0.2623.108 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/52.0.2743.100 YaBrowser/17.7.1.17 (lite) Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/53.0.2785.146 Mobile Safari/537.36 XiaoMi/MiuiBrowser/8.5.12",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/53.0.2785.146 Mobile Safari/537.36 XiaoMi/MiuiBrowser/8.7.0",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/53.0.2785.146 Mobile Safari/537.36 XiaoMi/MiuiBrowser/8.7.7",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/53.0.2785.146 Mobile Safari/537.36 XiaoMi/MiuiBrowser/8.8.2",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/53.0.2785.146 Mobile Safari/537.36 XiaoMi/MiuiBrowser/8.9.1",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/53.0.2785.146 Mobile Safari/537.36 XiaoMi/MiuiBrowser/9.1.3",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/53.0.2785.146 Mobile Safari/537.36 XiaoMi/MiuiBrowser/9.2.0",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/53.0.2785.146 Mobile Safari/537.36 XiaoMi/MiuiBrowser/9.2.3",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/55.0.2883.91 Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/59.0.3071.92 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/60.0.3112.116 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/60.0.3112.116 YaBrowser/17.7.1.17 (lite) Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/60.0.3112.78 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.3163.98 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.3163.98 Mobile Safari/537.36 OPR/29.0.2254.120398",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.3163.98 Mobile Safari/537.36 OPR/30.0.2254.121224",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.3163.98 Mobile Safari/537.36 OPR/31.0.2254.121393",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.3163.98 YaBrowser/17.7.1.17 (lite) Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/62.0.3202.45 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Focus/2.1 Chrome/51.0.2704.106 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Focus/2.1 Chrome/55.0.2883.91 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Focus/2.1 Chrome/57.0.2987.132 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Focus/2.1 Chrome/60.0.3112.78 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Focus/2.1 Chrome/61.0.3163.98 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 UCBrowser/11.4.2.995 U3/0.8.0 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 UCBrowser/11.4.5.1005 U3/0.8.0 Mobile Safari/537.36",
            "Mozilla/5.0 (DEVICE) Gecko/38.0 Firefox/38.0",
            "Mozilla/5.0 (DEVICE) Gecko/45.0 Firefox/45.0",
            "Mozilla/5.0 (DEVICE) Gecko/52.0 Firefox/52.0",
            "Mozilla/5.0 (DEVICE) Gecko/54.0 Firefox/54.0",
            "Mozilla/5.0 (DEVICE) Gecko/55.0 Firefox/55.0",
            "Mozilla/5.0 (DEVICE) Gecko/56.0 Firefox/56.0",
            "Mozilla/5.0 (DEVICE) Gecko/57.0 Firefox/57.0",
            "Mozilla/5.0 (DEVICE) Gecko/58.0 Firefox/58.0",
            "Opera/9.80 (DEVICE) Presto/2.11.355 Version/12.10",
            "Opera/9.80 (DEVICE) Presto/2.12.423 Version/12.16"
    };

    private String useragent() {
        String ua = System.getProperty("http.agent", "()");
        String device = ua.substring(ua.indexOf("("), ua.indexOf(")") + 1);
        ua = useragents[(int) Math.floor(random.nextInt(useragents.length))];
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
}
