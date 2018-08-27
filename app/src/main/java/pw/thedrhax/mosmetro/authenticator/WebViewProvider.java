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

import pw.thedrhax.mosmetro.services.WebViewService;
import pw.thedrhax.util.Logger;

/**
 * Base class for all WebView-specific providers.
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see Provider
 * @see Task
 */

public abstract class WebViewProvider extends Provider {

    public WebViewProvider(Context context) {
        super(context);
    }

    @Override
    public boolean init() {
        if (!super.init()) return false;

        Intent intent = new Intent(
                context, WebViewService.class
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (!context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
            Logger.log(this, "Can't connect to WebViewService");
            deinit();
            return false;
        }

        while (wv == null) {
            SystemClock.sleep(100);

            if (!running.get()) {
                deinit();
                return false;
            }
        }

        return true;
    }

    @Override
    public void deinit() {
        super.deinit();

        Logger.log(this, "Disconnecting from WebViewService");
        try {
            context.unbindService(connection);
        } catch (IllegalArgumentException ex) {
            Logger.log(Logger.LEVEL.DEBUG, ex);
        }
    }

    /*
     * Binding interface
     */

    protected WebViewService wv = null;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            wv = ((WebViewService.ScriptedWebViewBinder)iBinder).getService();
            wv.getRunningListener().subscribe(running);
            wv.setInterceptors(WebViewProvider.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (wv != null) {
                wv.getRunningListener().unsubscribe();
                wv = null;
            }
        }
    };
}
