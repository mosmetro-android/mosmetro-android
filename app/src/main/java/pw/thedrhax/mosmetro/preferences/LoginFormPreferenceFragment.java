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

package pw.thedrhax.mosmetro.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import pw.thedrhax.mosmetro.R;

public class LoginFormPreferenceFragment extends PreferenceDialogFragmentCompat {
    private final String key;

    private SharedPreferences settings;
    private EditText text_login;
    private EditText text_password;
    
    public LoginFormPreferenceFragment(LoginFormPreference preference) {
        key = preference.getKey();
        Bundle bundle = new Bundle(1);
        bundle.putString("key", key);
        setArguments(bundle);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    @Override
    protected View onCreateDialogView(Context context) {
        View view = getLayoutInflater().inflate(R.layout.loginform_preference, null);

        text_login = view.findViewById(R.id.text_login);
        text_password = view.findViewById(R.id.text_password);

        text_login.setText(settings.getString(key + "_login", ""));
        text_password.setText(settings.getString(key + "_password", ""));

        return view;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            settings.edit()
                    .putString(key + "_login", text_login.getText().toString())
                    .putString(key + "_password", text_password.getText().toString())
                    .apply();
        }
    }
}