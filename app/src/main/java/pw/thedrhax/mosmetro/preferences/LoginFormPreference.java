package pw.thedrhax.mosmetro.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import pw.thedrhax.mosmetro.R;

public class LoginFormPreference extends DialogPreference {
    private SharedPreferences settings;
    private EditText text_login;
    private EditText text_password;

    public LoginFormPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public LoginFormPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public LoginFormPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LoginFormPreference(Context context) {
        super(context);
        init(context);
    }
    
    private void init(Context context) {
        settings = PreferenceManager.getDefaultSharedPreferences(context);
    };

    @Override
    protected View onCreateDialogView() {
        View v = LayoutInflater
                .from(getContext())
                .inflate(R.layout.loginform_preference, null);

        text_login = (EditText) v.findViewById(R.id.text_login);
        text_login.setText(settings.getString(getKey() + "_login", ""));

        text_password = (EditText) v.findViewById(R.id.text_password);
        text_password.setText(settings.getString(getKey() + "_password", ""));

        return v;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult)
            settings.edit()
                    .putString(getKey() + "_login", text_login.getText().toString())
                    .putString(getKey() + "_password", text_password.getText().toString())
                    .apply();

        super.onDialogClosed(positiveResult);
    }
}