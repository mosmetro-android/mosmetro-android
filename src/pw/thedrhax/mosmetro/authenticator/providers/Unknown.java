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

package pw.thedrhax.mosmetro.authenticator.providers;

import android.content.Context;

import java.util.HashMap;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.authenticator.Task;

/**
 * The Unknown class is used to tell user that this provider is not
 * recognized or that the Internet connection is already available.
 *
 * Detection: Any response not recognized by all previous Provider classes.
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see Provider
 */

public class Unknown extends Provider {

    public Unknown(final Context context) {
        super(context);

        /**
         * Checking Internet connection for a first (and the last) time
         */
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                logger.log(context.getString(R.string.auth_checking_connection));

                if (isConnected()) {
                    logger.log(context.getString(R.string.auth_already_connected));
                    vars.put("result", RESULT.ALREADY_CONNECTED);
                } else {
                    logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_provider)
                    ));
                    vars.put("result", RESULT.NOT_SUPPORTED);
                }

                return false;
            }
        });
    }
}
