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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import pw.thedrhax.mosmetro.R;

/**
 * Activity used to call ACTION_VIEW safely
 * (it will not crash if no browsers are installed)
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 */
public class SafeViewActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getIntent().hasExtra("data")) {
            finish(); return;
        }

        Uri data = Uri.parse(getIntent().getStringExtra("data"));
        Intent intent = new Intent(Intent.ACTION_VIEW, data);

        if (getIntent().hasExtra("action")) {
            intent.setAction(getIntent().getStringExtra("action"));
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, R.string.toast_view_exception, Toast.LENGTH_LONG).show();
        }

        finish();
    }
}
