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

import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import com.topjohnwu.superuser.Shell;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import pw.thedrhax.mosmetro.authenticator.Gen204;

public class CaptivePortalFix {
    // Android < 7.1.1
    public static final String CAPTIVE_PORTAL_SERVER = "captive_portal_server";

    // Android >= 7.1.1
    public static final String CAPTIVE_PORTAL_HTTP_URL = "captive_portal_http_url";
    public static final String CAPTIVE_PORTAL_HTTPS_URL = "captive_portal_https_url";

    private final Context context;
    private final Randomizer random;

    public CaptivePortalFix(Context context) {
        this.context = context;
        this.random = new Randomizer(context);
    }

    public boolean isApplied() {
        ContentResolver resolver = context.getContentResolver();

        List<String> urls = Arrays.asList(Gen204.URL_RELIABLE);
        List<String> domains = new LinkedList<String>() {{
            for (String server : urls) {
                add(server.split("/")[0]);
            }
        }};

        if (Build.VERSION.SDK_INT >= 17) {
            String server = Settings.Global.getString(resolver, CAPTIVE_PORTAL_SERVER);
            if (server == null || !domains.contains(server)) return false;

            String http_url = Settings.Global.getString(resolver, CAPTIVE_PORTAL_HTTP_URL);
            if (http_url == null || !urls.contains(http_url.substring(7))) return false;

            String https_url = Settings.Global.getString(resolver, CAPTIVE_PORTAL_HTTPS_URL);
            if (https_url == null || !urls.contains(https_url.substring(8))) return false;

            return true;
        } else {
            return false;
        }
    }

    public void apply(Shell.ResultCallback result) {
        String url = (String) random.choose(Gen204.URL_RELIABLE);
        String domain = url.split("/")[0];

        Shell.sh(
                "settings put global " + CAPTIVE_PORTAL_SERVER + " " + domain,
                "settings put global " + CAPTIVE_PORTAL_HTTP_URL + " http://" + url,
                "settings put global " + CAPTIVE_PORTAL_HTTPS_URL + " https://" + url
        ).submit(result);
    }

    public void revert(Shell.ResultCallback result) {
        Shell.sh(
                "settings delete global " + CAPTIVE_PORTAL_SERVER,
                "settings delete global " + CAPTIVE_PORTAL_HTTP_URL,
                "settings delete global " + CAPTIVE_PORTAL_HTTPS_URL
        ).submit(result);
    }

    public void toggle(Shell.ResultCallback result) {
        if (isApplied()) {
            revert(result);
        } else {
            apply(result);
        }
    }
}
