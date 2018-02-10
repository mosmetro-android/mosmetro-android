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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;

import java.util.HashMap;

import pw.thedrhax.mosmetro.activities.ScriptedWebViewActivity;
import pw.thedrhax.util.Listener;

public class ScriptedWebViewTask implements Task {
    private static final String ACTION = "pw.thedrhax.mosmetro.authenticator.ScriptedWebViewTask";

    private Provider p;
    private Intent intent;

    @RequiresApi(19)
    public ScriptedWebViewTask(Provider p, String message, String url, String script) {
        this.p = p;

        this.intent = new Intent(p.context, ScriptedWebViewActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("url", url)
                .putExtra("script", script)
                .putExtra("callback", ACTION)
                .putExtra("message", message);
    }

    @Override
    public boolean run(HashMap<String, Object> vars) {
        final Listener<Boolean> stopped = new Listener<>(false);
        final Listener<Boolean> result = new Listener<>(false);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION.equals(intent.getAction())) {
                    result.set(!intent.hasExtra("result") || "SUCCESS".equals(intent.getStringExtra("result")));
                    stopped.set(true);
                }
            }
        };

        p.context.registerReceiver(receiver, new IntentFilter(ACTION));
        p.context.startActivity(intent);

        while (p.running.get() && !stopped.get()) {
            SystemClock.sleep(100);
        }

        p.context.unregisterReceiver(receiver);
        // TODO: Force finish Activity at this point
        return result.get();
    }
}
