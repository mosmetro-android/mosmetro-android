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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import io.sentry.Attachment;
import io.sentry.Sentry;
import io.sentry.android.core.SentryAndroid;
import io.sentry.protocol.User;
import pw.thedrhax.mosmetro.services.BackendWorker;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.UUID;
import pw.thedrhax.util.Version;

import com.topjohnwu.superuser.Shell;

import java.util.List;

public class MosMetroApp extends Application {
    static {
        Shell.setDefaultBuilder(Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
        );
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.configure(this);
        BackendWorker.configure(this);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        SentryAndroid.init(this, options -> {
            options.setDsn("https://13509f0e75f74081845cfe990b9840f3@o1176364.ingest.sentry.io/4504074406199296");
            options.setEnableAutoSessionTracking(false);
            options.setRelease(Version.getFormattedVersion());
            options.setTag("branch", Version.getBranch());
            options.setTag("build", "" + Version.getBuildNumber());

            options.setBeforeSend((event, hint) -> {
                boolean manual = "true".equals(event.getTag("manual"));

                if (!manual && !settings.getBoolean("acra.enable", true)) {
                    return null;
                }

                if (manual || settings.getBoolean("pref_debug_last_log", true)) {
                    StringBuilder cropped_log = new StringBuilder();
                    List<CharSequence> full_log = Logger.read(Logger.LEVEL.DEBUG);
                    int cut = full_log.lastIndexOf(Logger.CUT);

                    for (CharSequence line : full_log.subList(cut + 1, full_log.size())) {
                        cropped_log.append(line).append('\n');
                    }

                    hint.addAttachment(new Attachment(cropped_log.toString().getBytes(), "log-debug.txt"));
                }

                return event;
            });
        });

        User user = new User();
        user.setId(UUID.get(this));
        Sentry.setUser(user);
    }
}
