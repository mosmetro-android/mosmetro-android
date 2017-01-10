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
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;

import pw.thedrhax.mosmetro.BuildConfig;

public final class PermissionUtils {
    private final WeakReference<Context> context;
    private final PowerManager pm;

    public PermissionUtils(@NonNull Context context) {
        this.context = new WeakReference<>(context);
        pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }

    /** Battery saving permissions. */
    public boolean isBatterySavingIgnored() {
        if (Build.VERSION.SDK_INT >= 23)
            return pm.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID);
        else
            return true;
    }

    public void requestBatterySavingIgnore() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (context.get() != null) {
                context.get().startActivity(new Intent()
                    .setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:" + context.get().getPackageName()))
                );
            }
        }
    }

    public void openBatterySavingSettings() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (context.get() != null) {
                context.get().startActivity(new Intent()
                    .setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                );
            }
        }
    }
}
