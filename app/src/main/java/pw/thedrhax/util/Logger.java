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
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import androidx.core.content.FileProvider;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pw.thedrhax.mosmetro.R;

public class Logger {
    public static final String CUT = "--------[ cut here ]--------";

    public enum LEVEL {INFO, DEBUG}

    private static final Map<LEVEL,LogWriter> logs = new HashMap<LEVEL,LogWriter>() {{
        for (LEVEL level : LEVEL.values()) {
            put(level, new LogWriter());
        }
    }};

    private static long last_timestamp = 0;

    private static String timestamp() {
        long diff = System.currentTimeMillis() - last_timestamp;
        last_timestamp = System.currentTimeMillis();
        return diff > 9999 ? "[+>10s]" : String.format(Locale.ENGLISH, "[+%04d]", diff);
    }

    /*
     * Configuration
     */

    private static boolean pref_debug_logcat = false;

    public static void configure(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        pref_debug_logcat = settings.getBoolean("pref_debug_logcat", false);

        for (LEVEL level : LEVEL.values()) {
            if (logs.containsKey(level)) {
                logs.get(level).close();
                logs.remove(level);
            }

            LogWriter writer = new LogWriter(
                context.getFilesDir(),
                "log-" + level.toString().toLowerCase() + ".txt",
                level == LEVEL.INFO ? 100 : 2000
            );

            logs.put(level, writer);
        }

        log(LEVEL.DEBUG, CUT);
    }

    /*
     * Inputs
     */

    public static void log (LEVEL level, String message) {
        synchronized (logs) {
            if (level == LEVEL.DEBUG && message != CUT) {
                if (pref_debug_logcat) {
                    Log.d("pw.thedrhax.mosmetro", message);
                }
                message = timestamp() + " " + message;
            }
            if (logs.containsKey(level)) {
                logs.get(level).add(message);
            }
        }
        onUpdate(level, message);
    }

    public static void log (LEVEL level, Throwable ex) {
        // getStackTraceString ignores DNS errors
        if (ex instanceof UnknownHostException) {
            log(level, ex.toString());
        } else {
            log(level, Log.getStackTraceString(ex));
        }
    }

    public static void log (String message) {
        for (LEVEL level : LEVEL.values()) {
            log(level, message);
        }
    }

    public static void log (Object obj, String message) {
        log(LEVEL.DEBUG, String.format(Locale.ENGLISH, "%s (%d) | %s",
                obj.getClass().getSimpleName(),
                System.identityHashCode(obj),
                message
        ));
    }

    /*
     * Logger Utils
     */

    public static void date(String prefix) {
        log(prefix + DateFormat.getDateTimeInstance().format(new Date()));
    }

    public static void wipe() {
        synchronized (logs) {
            for (LogWriter writer : logs.values()) {
                writer.clear();
            }
        }
    }

    /*
     * Outputs
     */

    public static LinkedList<String> read(LEVEL level) {
        synchronized (logs) {
            return new LinkedList<>(logs.get(level));
        }
    }

    public static String toString(LEVEL level) {
        StringBuilder result = new StringBuilder();
        for (String message : read(level)) {
            result.append(message).append("\n");
        }
        return result.toString();
    }

    /**
     * Log file writer
     */

    public static class LogWriter extends LinkedList<String> {
        private File file;
        private FileWriter writer = null;

        private static List<String> tail(File file, int lines) {
            List<String> history = new LinkedList<>();

            if (!file.exists()) return history;

            try (FileReader is = new FileReader(file)) {
                BufferedReader reader = new BufferedReader(is);

                String line;
                while ((line = reader.readLine()) != null) {
                    history.add(line);
                }
            } catch (IOException ignored) {}

            if (history.size() > lines) {
                int start = history.size() - lines, end = history.size();
                history = history.subList(start, end);
            }

            return history;
        }

        public LogWriter(File dir, String filename, int truncate) {
            file = new File(dir, filename);

            List<String> history = tail(file, truncate);

            clear();

            if (history.size() > 0) {
                addAll(history);
            }
        }

        public LogWriter() {
            file = null;
        }

        @Override
        public boolean add(String e) {
            try {
                if (writer != null) {
                    writer.write(e + '\n');
                    writer.flush();
                }
            } catch (IOException ignored) {}
            return super.add(e);
        }

        @Override
        public boolean addAll(Collection<? extends String> c) {
            for (String line : c) {
                try {
                    if (writer != null) writer.write(line + '\n');
                } catch (IOException ignored) {}
            }

            try {
                if (writer != null) writer.flush();
            } catch (IOException ignored) {}            

            return super.addAll(c);
        }

        @Override
        public void clear() {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {}
                writer = null;
            }

            if (file != null) {
                try {
                    writer = new FileWriter(file, false);
                } catch (IOException ignored) {}
            }

            super.clear();
        }

        public void close() {
            try {
                if (writer != null) {
                    writer.close();
                    writer = null;
                }
            } catch (IOException ignored) {}
        }
    }

    /**
     * Log sharing routines
     */

    public static Uri writeToFile(Context context) throws IOException {
        File log_file = new File(context.getFilesDir(), "pw.thedrhax.mosmetro.txt");

        FileWriter writer = new FileWriter(log_file);
        writer.write(toString(Logger.LEVEL.DEBUG));
        writer.flush(); writer.close();

        return FileProvider.getUriForFile(context, "pw.thedrhax.mosmetro.provider", log_file);
    }

    public static void share(Context context) {
        Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_EMAIL,
                        new String[] {context.getString(R.string.report_email_address)}
                )
                .putExtra(Intent.EXTRA_SUBJECT,
                        context.getString(R.string.report_email_subject, Version.getFormattedVersion())
                );

        try {
            share.putExtra(Intent.EXTRA_STREAM, writeToFile(context));
        } catch (IOException ex) {
            Logger.log(Logger.LEVEL.DEBUG, ex);
            Logger.log(context.getString(R.string.error, context.getString(R.string.error_log_file)));
            share.putExtra(Intent.EXTRA_TEXT, read(Logger.LEVEL.DEBUG));
        }

        context.startActivity(Intent.createChooser(
                share, context.getString(R.string.report_choose_client)
        ));
    }

    /**
     * Callback interface
     *
     * This abstract class will receive messages from another thread
     * and forward them safely to the current thread, so it can be
     * used with the Android UI.
     */
    public static abstract class Callback {

        /**
         * Handler object used to forward messages between threads
         */
        private Handler handler;

        protected Callback() {
            handler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    log(
                            LEVEL.valueOf(msg.getData().getString("level")),
                            msg.getData().getString("message")
                    );
                    return true;
                }
            });
        }

        /**
         * This method is being called from another thread
         * @param level One of values stored in LEVEL enum
         * @param message Text of the message being forwarded
         */
        void call(LEVEL level, String message) {
            Bundle data = new Bundle();
            data.putString("level", level.name());
            data.putString("message", message);

            Message msg = new Message();
            msg.setData(data);

            handler.sendMessage(msg);
        }

        /**
         * This method must be implemented by all sub classes
         * to be able to receive the forwarded messages.
         * @param level One of values stored in LEVEL enum
         * @param message Text of the message being forwarded
         */
        public abstract void log(LEVEL level, String message);
    }

    /**
     * Map of registered Callback objects
     */
    private static Map<Object,Callback> callbacks = new HashMap<>();

    /**
     * Register the Callback object in the Logger
     * @param key Any object used to identify the Callback
     * @param callback Callback object to be registered
     */
    public static void registerCallback(Object key, Callback callback) {
        callbacks.put(key, callback);
    }

    /**
     * Unregister the Callback object
     * @param key Any object used to identify the Callback
     */
    public static void unregisterCallback(Object key) {
        callbacks.remove(key);
    }

    /**
     * Get the registered Callback by it's key object
     * @param key Any object used to identify the Callback
     * @return Callback object identified by this key
     */
    public static Callback getCallback(Object key) {
        return callbacks.get(key);
    }

    private static void onUpdate(LEVEL level, String message) {
        for (Callback callback : callbacks.values()) {
            callback.call(level, message);
        }
    }
}
