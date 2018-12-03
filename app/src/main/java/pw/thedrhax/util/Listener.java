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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Util class used to monitor every change of the stored variable.
 *
 * Main functions:
 *   - Subscribe to already existing Listeners of the same type
 *   - Allow to retrieve and change the value of variable at any time
 *   - Notify about every change using the onChange() callback
 *   - Debounce value changes (Source: https://stackoverflow.com/a/38296055)
 *   - Stack Overflow protection by checking if child is the master at the same time
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @param <T> type of the stored variable
 */
public class Listener<T> {
    private final Queue<Listener<T>> masters = new ConcurrentLinkedQueue<>();
    private final Queue<Listener<T>> callbacks = new ConcurrentLinkedQueue<>();
    private T value;

    private int debounce_ms = 0;
    private Future<?> last_call = null;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();


    public Listener(T initial_value) {
        value = initial_value;
    }

    public final Listener<T> debounce(int time_ms) {
        debounce_ms = time_ms; return this;
    }

    public final synchronized void set(T new_value) {
        value = new_value;

        if (debounce_ms == 0) {
            onChange(new_value);
        } else {
            Future<?> prev_call = last_call;

            last_call = scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    onChange(new_value);
                    last_call = null;
                }
            }, debounce_ms, TimeUnit.MILLISECONDS);

            if (prev_call != null) {
                prev_call.cancel(true);
            }
        }

        for (Listener<T> callback : callbacks) {
            if (callback.callbacks.contains(this)) {
                callback.value = new_value;
                callback.onChange(new_value);
            } else {
                callback.set(new_value);
            }
        }
    }

    public final T get() {
        return value;
    }

    public void subscribe(Listener<T> master) {
        if (!master.callbacks.contains(this)) {
            master.callbacks.add(this);
        }
        if (!masters.contains(master)) {
            masters.add(master);
        }
        this.value = master.value;
    }

    public void unsubscribe() {
        for (Listener<T> master : masters) {
            unsubscribe(master);
        }
    }

    public void unsubscribe(Listener<T> master) {
        if (master.callbacks.contains(this)) {
            master.callbacks.remove(this);
        }
        if (masters.contains(master)) {
            masters.remove(master);
        }
    }

    public void onChange(T new_value) {}
}
