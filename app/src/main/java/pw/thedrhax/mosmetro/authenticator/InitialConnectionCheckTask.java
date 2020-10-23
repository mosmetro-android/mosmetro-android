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

import java.util.HashMap;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.httpclient.ParsedResponse;
import pw.thedrhax.util.Logger;

public abstract class InitialConnectionCheckTask implements Task {
    private Provider p;
    private ParsedResponse res;
    private boolean first_start = true;

    public InitialConnectionCheckTask(Provider p, ParsedResponse res) {
        this.p = p;
        this.res = res;
    }

    @Override
    public boolean run(HashMap<String, Object> vars) {
        ParsedResponse response = res;

        if (!first_start) {
            Logger.log(p.context.getString(R.string.auth_checking_connection));
            response = p.gen_204.check(true);
        }

        first_start = false;

        if (Provider.isConnected(response)) {
            Logger.log(p.context.getString(R.string.auth_already_connected));
            vars.put("result", Provider.RESULT.ALREADY_CONNECTED);
            return false;
        }

        return handle_response(vars, response);
    }

    public abstract boolean handle_response(HashMap<String, Object> vars, ParsedResponse response);
}
