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

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Logger {
    public enum LEVEL {INFO, DEBUG}
    private Map<LEVEL,StringBuilder> logs;

    private static Logger instance;

    public static synchronized Logger getLogger() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    private Logger () {
        logs = new HashMap<>();
        for (LEVEL level : LEVEL.values()) {
            logs.put(level, new StringBuilder());
        }
    }

    /*
     * Inputs
     */

    public void log (LEVEL level, String message) {
        logs.get(level).append(message).append("\n");
        for (Callback callback : callbacks.values())
            callback.call(level, message);
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

    public void date() {
        log(DateFormat.getDateTimeInstance().format(new Date()));
    }

    public void wipe() {
        for (LEVEL level : LEVEL.values()) {
            logs.put(level, new StringBuilder());
        }
    }

    /*
     * Outputs
     */

    public String get (LEVEL level) {
        return logs.get(level).toString();
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
    private Map<Object,Callback> callbacks = new HashMap<>();

    /**
     * Register the Callback object in the Logger
     * @param key Any object used to identify the Callback
     * @param callback Callback object to be registered
     * @return Current Logger's instance
     */
    public Logger registerCallback(Object key, Callback callback) {
        callbacks.put(key, callback); return this;
    }

    /**
     * Unregister the Callback object
     * @param key Any object used to identify the Callback
     * @return Current Logger's instance
     */
    public Logger unregisterCallback(Object key) {
        callbacks.remove(key); return this;
    }

    /**
     * Get the registered Callback by it's key object
     * @param key Any object used to identify the Callback
     * @return Callback object identified by this key
     */
    public Callback getCallback(Object key) {
        return callbacks.get(key);
    }
}
