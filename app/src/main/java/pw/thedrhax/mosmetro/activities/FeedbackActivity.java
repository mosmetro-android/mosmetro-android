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
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import io.sentry.Sentry;
import io.sentry.UserFeedback;
import io.sentry.protocol.SentryId;
import pw.thedrhax.mosmetro.R;

public class FeedbackActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedback_activity);
    }

    public void buttonCancel(View view) {
        finish();
    }

    public void buttonSend(View view) {
        EditText text_message = findViewById(R.id.text_message);
        String message = text_message.getText().toString();

        Sentry.setTag("manual", "true");
        SentryId id = Sentry.captureMessage("Manual report");
        Sentry.removeTag("manual");

        if (message.length() > 0) {
            UserFeedback feedback = new UserFeedback(id);
            feedback.setComments(message);
            Sentry.captureUserFeedback(feedback);
        }

        Toast.makeText(this, R.string.done, Toast.LENGTH_SHORT).show();
        finish();
    }
}
