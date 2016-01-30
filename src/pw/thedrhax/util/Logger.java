package pw.thedrhax.util;

public class Logger {
    private StringBuilder log, debug;

    public Logger () {
        log = new StringBuilder();
        debug = new StringBuilder();
    }

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

    public String getLog() {
        return log.toString();
    }

    public String getDebug() {
        return debug.toString();
    }
}
