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
import androidx.work.Configuration;
import androidx.work.WorkManager;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;

import pw.thedrhax.mosmetro.acra.CustomHttpSenderFactory;
import pw.thedrhax.mosmetro.services.ScheduledWorker;
import pw.thedrhax.util.Logger;

import static org.acra.ReportField.*;

@AcraCore(
        reportContent = {
            // Required by Tracepot
            ANDROID_VERSION, APP_VERSION_CODE, APP_VERSION_NAME,
            PACKAGE_NAME, REPORT_ID, STACK_TRACE, USER_APP_START_DATE,
            USER_CRASH_DATE,

            // Additional info
            INSTALLATION_ID, BUILD_CONFIG, PHONE_MODEL, CUSTOM_DATA,
            APPLICATION_LOG
        },
        reportSenderFactoryClasses = {CustomHttpSenderFactory.class},
        applicationLogFile = "log-debug.txt",
        applicationLogFileLines = 1000)
public class MosMetroApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        WorkManager.initialize(this,
                new Configuration.Builder()
                        .setMinimumLoggingLevel(android.util.Log.DEBUG)
                        .build()
        );
        ScheduledWorker.configure(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        ACRA.init(this);

        if (!ACRA.isACRASenderServiceProcess()) {
            Logger.configure(base);
        }
    }
}
