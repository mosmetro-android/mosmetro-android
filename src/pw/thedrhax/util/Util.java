package pw.thedrhax.util;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Util {
    // Convert Exception's printStackTrace() to String
    public static String exToStr (Exception ex) {
        StringWriter wr = new StringWriter();
        ex.printStackTrace(new PrintWriter(wr));
        return wr.toString();
    }

    // JavaScript evaluation
    public static String js (String script, String result_var) {
        Context context = Context.enter();
        Scriptable scope = context.initStandardObjects();
        context.evaluateString(scope, script, "", 1, null);
        return Context.toString(scope.get(result_var, scope));
    }
}
