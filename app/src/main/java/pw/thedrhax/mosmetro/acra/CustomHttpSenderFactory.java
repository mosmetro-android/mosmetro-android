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

package pw.thedrhax.mosmetro.acra;

import android.content.Context;
import androidx.annotation.NonNull;
import pw.thedrhax.mosmetro.BuildConfig;

import org.acra.config.CoreConfiguration;
import org.acra.data.StringFormat;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderFactory;
import org.acra.sender.HttpSender.Method;

public class CustomHttpSenderFactory implements ReportSenderFactory {

    @Override
    public boolean enabled(@NonNull CoreConfiguration coreConfiguration) {
        return true;
    }

    @NonNull
    @Override
    public ReportSender create(@NonNull Context context, @NonNull CoreConfiguration config) {
        return new CustomHttpSender(
                config,
                Method.POST,
                StringFormat.JSON, 
                "https://collector.tracepot.com/" + BuildConfig.TRACEPOT_ID
        );
    }
}