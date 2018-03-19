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

/**
 * Util class used to monitor every change of the stored variable.
 *
 * Main functions:
 *   - Subscribe to already existing Listeners of the same type
 *   - Allow to retrieve and change the value of variable at any time
 *   - Notify about every change using the onChange() callback
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @param <T> type of the stored variable
 */
public class Listener<T> {
    private T value;
    private final Queue<Listener<T>> callbacks = new ConcurrentLinkedQueue<>();

    public Listener(T initial_value) {
        value = initial_value;
    }

    public final synchronized void set(T new_value) {
        value = new_value;
        onChange(new_value);
        for (Listener<T> callback : callbacks) {
            callback.set(new_value);
        }
    }

    public final T get() {
        return value;
    }

    public void subscribe(Listener<T> master) {
        master.callbacks.add(this);
        this.value = master.value;
    }

    public void unsubscribe(Listener<T> master) {
        master.callbacks.remove(this);
    }

    public void onChange(T new_value) {

    }
}
