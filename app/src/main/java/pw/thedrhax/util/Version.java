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

import androidx.annotation.NonNull;

import java.util.Locale;

import pw.thedrhax.mosmetro.BuildConfig;

/**
 * The Version util class is used to get formatted application version
 * information from build-time constants.
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @author Savelii Zagurskii <saveliyzagurskiy@gmail.com>
 */
public final class Version {
    private Version() {}

    @NonNull public static String getVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    public static int getVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    @NonNull public static String getFormattedVersion() {
        if (getBranch().equals("play")) {
            return getVersionName();
        } else {
            return String.format(Locale.ENGLISH,"%s #%d", getBranch(), getBuildNumber());
        }
    }

    @NonNull public static String getBranch() {
        return BuildConfig.BRANCH_NAME;
    }

    public static int getBuildNumber() {
        return BuildConfig.BUILD_NUMBER;
    }
}
