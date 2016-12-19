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

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Logger implements Parcelable {
    public enum LEVEL {
        INFO,
        DEBUG,
    }
    private Map<LEVEL,StringBuilder> logs;

    public Logger () {
        logs = new HashMap<LEVEL, StringBuilder>();
        for (LEVEL level : LEVEL.values()) {
            logs.put(level, new StringBuilder());
        }
    }

    /*
     * Inputs
     */

    public void log (LEVEL level, String message) {
        logs.get(level).append(message).append("\n");
    }

    public void log (LEVEL level, Throwable ex) {
        log(level, Log.getStackTraceString(ex));
    }

    public void log (String message) {
        for (LEVEL level : LEVEL.values()) {
            log(level, message);
        }
    }

    /*
     * Logger Utils
     */

    public void merge (Logger logger) {
        for (LEVEL level : LEVEL.values()) {
            log(level, logger.get(level));
        }
    }

    public void date() {
        log(DateFormat.getDateTimeInstance().format(new Date()));
    }

    /*
     * Outputs
     */

    public String get (LEVEL level) {
        return logs.get(level).toString();
    }

    /*
     * Implementation of Parcelable
     */

    public static Parcelable.Creator CREATOR = new Parcelable.Creator<Logger>() {
        @Override
        public Logger[] newArray(int size) {
            return new Logger[size];
        }

        @Override
        public Logger createFromParcel(Parcel source) {
            Logger logger = new Logger();
            for (LEVEL level : LEVEL.values()) {
                logger.log(level, source.readString());
            }
            return logger;
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        for (LEVEL level : LEVEL.values()) {
            dest.writeString(get(level));
        }
    }
}
