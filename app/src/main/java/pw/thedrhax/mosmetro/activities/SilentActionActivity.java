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

package pw.thedrhax.mosmetro.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;

import pw.thedrhax.util.Notify;

public class SilentActionActivity extends Activity {
    public static final String ACTION_DISABLE_DONATE_REMINDER = "disable-donate-reminder";
    public static final String ACTION_TOGGLE_DONATE_REMINDER_FREQUENCY = "donate-reminder-freq";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Intent intent = getIntent();

        if (intent == null) {
            finish();
            return;
        }

        if (ACTION_DISABLE_DONATE_REMINDER.equals(intent.getAction())) {
            settings.edit().putBoolean("pref_notify_donate", false).apply();
            new Notify(this).id(128).hide();
        }

        if (ACTION_TOGGLE_DONATE_REMINDER_FREQUENCY.equals(intent.getAction())) {
            settings.edit().putBoolean("pref_notify_donate_freq", true).apply();
            new Notify(this).id(128).hide();
        }

        finish();
    }
}
