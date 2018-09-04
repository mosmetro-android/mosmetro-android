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
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

import pw.thedrhax.mosmetro.R;

public class ShortcutDialog extends Activity {
    private CheckBox check_background;
    private CheckBox check_force;
    private CheckBox check_stop;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shortcut_activity);

        check_background = (CheckBox)findViewById(R.id.check_background);
        check_force = (CheckBox)findViewById(R.id.check_force);
        check_stop = (CheckBox)findViewById(R.id.check_stop);
    }

    public void check_background(View view) {
        boolean checked = check_background.isChecked();
        check_force.setEnabled(checked);

        if (!checked)
            check_force.setChecked(false);
    }

    public void check_stop(View view) {
        boolean checked = check_stop.isChecked();
        check_background.setEnabled(!checked);

        if (checked) {
            check_background.setChecked(false);
            check_background(check_background);
        }
    }

    private String getShortcutName() {
        if (check_background.isChecked()) {
            return getString(R.string.in_background);
        } else if (check_stop.isChecked()) {
            return getString(R.string.stop);
        } else {
            return getString(R.string.connect);
        }
    }

    private Class getActivityClass() {
        if (check_background.isChecked() || check_stop.isChecked()) {
            return ConnectionServiceActivity.class;
        } else {
            return DebugActivity.class;
        }
    }

    public void button_save(View view) {
        Intent result = new Intent();

        Intent shortcut_intent = new Intent(this, getActivityClass())
                .putExtra("force", check_force.isChecked())
                .putExtra("stop", check_stop.isChecked());

        result.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcut_intent);
        result.putExtra(Intent.EXTRA_SHORTCUT_NAME, getShortcutName());
        result.putExtra(
                Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher)
        );

        if ("android.intent.action.CREATE_SHORTCUT".equals(getIntent().getAction())) {
            setResult(RESULT_OK, result);
        } else {
            result.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            sendBroadcast(result);
        }

        finish();
    }

    public void button_cancel (View view) {
        finish();
    }
}
