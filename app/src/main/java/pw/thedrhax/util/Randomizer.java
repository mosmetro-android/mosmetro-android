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
import android.os.SystemClock;

import java.security.SecureRandom;

/**
 * This class is used to get random values and delays.
 */
public class Randomizer {
    private SecureRandom random = new SecureRandom();

    private int delay_min = 0;
    private int delay_max = 0;

    public Randomizer(Context context) {
        delay_min = Util.getIntPreference(context, "pref_delay_min", 1);
        delay_max = Util.getIntPreference(context, "pref_delay_max", 3);
        if (delay_min > delay_max) { // User tries to break everything again...
            int temp = delay_max;
            delay_max = delay_min;
            delay_min = temp;
        }
    }

    public void delay(Listener<Boolean> running) {
        int delay = delay_min;
        if (delay_max - delay_min != 0) {
            delay += random.nextInt(1000 * (delay_max - delay_min));
        }

        while (delay >= 100 && running.get()) {
            delay -= 100;
            SystemClock.sleep(100);
        }

        if (delay > 0 && running.get()) {
            SystemClock.sleep(delay);
        }
    }
}
