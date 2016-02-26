package pw.thedrhax.util;

import android.os.Parcel;
import android.os.Parcelable;

public class Logger implements Parcelable {
    private StringBuilder log, debug;

    public Logger () {
        log = new StringBuilder();
        debug = new StringBuilder();
    }

    /*
     * Inputs
     */

    public void log (String message) {
        log.append(message).append("\n");
    }

    public void debug (String message) {
        debug.append(message).append("\n");
    }

    public void debug (Exception ex) {
        debug(Util.exToStr(ex));
    }

    public void log_debug (String message) {
        log(message);
        debug(message);
    }

    public void merge (Logger logger) {
        log(logger.getLog());
        debug(logger.getDebug());
    }

    /*
     * Outputs
     */

    public String getLog() {
        return log.toString();
    }

    public String getDebug() {
        return debug.toString();
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
            logger.log(source.readString());
            logger.debug(source.readString());
            return logger;
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(log.toString());
        dest.writeString(debug.toString());
    }
}
