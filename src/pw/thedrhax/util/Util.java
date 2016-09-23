package pw.thedrhax.util;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class Util {
    // JavaScript evaluation
    public static String js (String script, String result_var) {
        Context context = Context.enter();
        Scriptable scope = context.initStandardObjects();
        context.evaluateString(scope, script, "", 1, null);
        return Context.toString(scope.get(result_var, scope));
    }
}
