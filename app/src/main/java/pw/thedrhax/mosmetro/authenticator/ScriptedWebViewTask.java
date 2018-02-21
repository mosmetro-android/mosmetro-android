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

package pw.thedrhax.mosmetro.authenticator;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import java.util.HashMap;

import pw.thedrhax.mosmetro.services.ScriptedWebViewService;

public abstract class ScriptedWebViewTask implements Task {
    private Provider p;

    @RequiresApi(19)
    protected ScriptedWebViewTask(Provider p) {
        this.p = p;
    }

    @Override
    public boolean run(HashMap<String, Object> vars) {
        Intent intent = new Intent(
                p.context, ScriptedWebViewService.class
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        p.context.bindService(intent, connection, Context.BIND_AUTO_CREATE);

        while (service == null) {
            SystemClock.sleep(100);

            if (!p.running.get()) {
                return false;
            }
        }

        boolean result = script(service);

        p.context.unbindService(connection);

        return result;
    }

    public abstract boolean script(@NonNull ScriptedWebViewService wv);

    /*
     * Binding interface
     */

    private ScriptedWebViewService service = null;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            service = ((ScriptedWebViewService.ScriptedWebViewBinder)iBinder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            service = null;
        }
    };
}
