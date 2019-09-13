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

package pw.thedrhax.mosmetro;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.data.StringFormat;

import pw.thedrhax.mosmetro.acra.HockeySenderFactory;
import pw.thedrhax.util.Logger;

import static org.acra.ReportField.*;

@AcraCore(buildConfigClass = BuildConfig.class,
          reportSenderFactoryClasses = {HockeySenderFactory.class},
          reportFormat = StringFormat.JSON,
          reportContent = {
                   INSTALLATION_ID, USER_EMAIL, USER_COMMENT, BUILD_CONFIG,
                   PACKAGE_NAME, ANDROID_VERSION, PHONE_MODEL, STACK_TRACE,
                   APPLICATION_LOG
          },
          applicationLogFile = "log-debug.txt",
          applicationLogFileLines = 1000)
public class MosMetroApp extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        ACRA.init(this);

        if (!ACRA.isACRASenderServiceProcess()) {
            Logger.configure(base);
        }
    }
}
