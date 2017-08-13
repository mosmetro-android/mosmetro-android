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
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.util.Util;

public class CaptchaDialog extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.captcha_dialog);
        setFinishOnTouchOutside(false);

        final CheckBox pref_captcha_dialog = (CheckBox) findViewById(R.id.pref_captcha_dialog);
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        pref_captcha_dialog.setChecked(settings.getBoolean("pref_captcha_dialog", true));
        pref_captcha_dialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settings.edit()
                        .putBoolean("pref_captcha_dialog", ((CheckBox)v).isChecked())
                        .apply();
            }
        });

        final Button submit_button = (Button) findViewById(R.id.submit_button);
        final EditText text_captcha = (EditText) findViewById(R.id.text_captcha);
        text_captcha.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    submit_button.performClick();
                    return true;
                }
                return false;
            }
        });
        text_captcha.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                text_captcha.removeTextChangedListener(this);
                int pos = text_captcha.getSelectionEnd();
                text_captcha.setText(Util.convertCyrillicSymbols(text_captcha.getText().toString()));
                text_captcha.setSelection(pos);
                text_captcha.addTextChangedListener(this);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        text_captcha.setText(getIntent().getStringExtra("code"));
        text_captcha.setSelection(0, 4);

        final ImageView image_captcha = (ImageView) findViewById(R.id.image_captcha);
        Bitmap image = Util.base64ToBitmap(getIntent().getStringExtra("image"));
        image_captcha.setImageBitmap(image);

        submit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBroadcast(new Intent("pw.thedrhax.mosmetro.event.CAPTCHA_RESULT")
                        .putExtra("value", text_captcha.getText().toString())
                        .putExtra("image", getIntent().getStringExtra("image"))
                );
                finish();
            }
        });

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if ("STOP".equals(intent.getAction()))
            finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sendBroadcast(
                new Intent("pw.thedrhax.mosmetro.event.CAPTCHA_RESULT").putExtra("value", "")
        );
    }
}
