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
