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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class Version {
    private PackageInfo pInfo = null;

    public Version (Context context) {
        try {
            pInfo = context
                    .getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException ignored) {}
    }

    public String getVersionName() {
        return (pInfo != null) ? pInfo.versionName : "unknown";
    }

    public int getVersionCode() {
        return (pInfo != null) ? pInfo.versionCode : Integer.MAX_VALUE;
    }

    public String getFormattedVersion() {
        return getVersionName() + "-" + getVersionCode();
    }
}
