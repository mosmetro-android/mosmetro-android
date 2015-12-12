package ru.thedrhax.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Util {
    // Convert Exception's printStackTrace() to String
    public static String exToStr (Exception ex) {
        StringWriter wr = new StringWriter();
        ex.printStackTrace(new PrintWriter(wr));
        return wr.toString();
    }
}
